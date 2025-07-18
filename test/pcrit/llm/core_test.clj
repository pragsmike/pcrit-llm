(ns pcrit.llm.core-test
  (:require [clojure.test :refer :all]
            [pcrit.llm.core :as llm]
            [pcrit.llm.costs :as costs]
            [clojure.data.json :as json]))

(deftest parse-llm-response-test
  (testing "Successfully parsing a valid response and calculating cost"
    (let [response {:body (json/write-str {:choices [{:message {:content "Hello, world!"}}]
                                           :usage {:prompt_tokens 10, :completion_tokens 5}})}
          mock-cost 0.123]
      (with-redefs [costs/calculate-cost (fn [model tin tout]
                                           (is (= "openai/gpt-test" model))
                                           (is (= 10 tin))
                                           (is (= 5 tout))
                                           mock-cost)]
        (let [result (llm/parse-llm-response response "openai/gpt-test")]
          (is (= "Hello, world!" (:content result)))
          (is (= :openai (get-in result [:generation-metadata :provider])))
          (is (= 10 (get-in result [:generation-metadata :token-in])))
          (is (= 5 (get-in result [:generation-metadata :token-out])))
          (is (= mock-cost (get-in result [:generation-metadata :cost-usd-snapshot])))))))

  (testing "Handling a response with no content returns an error map"
    (let [response {:body (json/write-str {:choices [{:message {:role "assistant"}}]})}
          result (llm/parse-llm-response response "test-model")]
      (is (nil? (:content result)))
      (is (string? (:error result)))))

  (testing "Handling malformed JSON returns an error map"
    (let [response {:body "{\"choices\": [{\"message\": "}
          result (llm/parse-llm-response response "test-model")]
      (is (nil? (:content result)))
      (is (string? (:error result))))))

(deftest call-model-test
  (let [last-call (atom nil)
        mock-post-fn (fn [url options]
                       (reset! last-call {:url url :options options})
                       {:status 200
                        :body (json/write-str {:choices [{:message {:content "Mock response"}}]
                                               :usage {:prompt_tokens 150, :completion_tokens 50}})})]

    (testing "Successful API call returns a structured map with full generation metadata"
      (with-redefs [costs/calculate-cost (constantly 0.999)]
        (let [result (llm/call-model "openai/mock-model" "A prompt" :post-fn mock-post-fn)]
          (is (= "Mock response" (:content result)))
          (is (some? (:generation-metadata result)))

          (let [gen-meta (:generation-metadata result)]
            (is (= "openai/mock-model" (:model gen-meta)))
            (is (= :openai (:provider gen-meta)))
            (is (= 150 (:token-in gen-meta)))
            (is (= 50 (:token-out gen-meta)))
            (is (= 0.999 (:cost-usd-snapshot gen-meta))) ; Verifies cost was calculated
            (is (integer? (:duration-ms gen-meta))))))))

  (testing "API returns an error status"
    (let [error-mock (fn [_url _options] {:status 500 :body "Server error"})
          result (llm/call-model "error-model" "A prompt" :post-fn error-mock)]
      (is (string? (:error result)))))

  (testing "HTTP client throws an exception"
    (let [exception-mock (fn [_url _options] (throw (Exception. "Connection timeout")))
          result (llm/call-model "exception-model" "A prompt" :post-fn exception-mock)]
      (is (string? (:error result)))
      (is (= "Network or client exception: Connection timeout" (:error result))))))
