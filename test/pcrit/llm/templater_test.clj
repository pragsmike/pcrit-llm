(ns pcrit.llm.templater-test
  (:require [clojure.test :refer :all]
            [pcrit.llm.templater :as templater]))

(deftest expand-test
  (testing "Replaces template variables correctly"
    (is (= "Hello, World!" (templater/expand "Hello, {{name}}!" {:name "World"})))
    (is (= "1 plus 2 equals 3" (templater/expand "{{a}} plus {{b}} equals {{c}}" {:a 1 :b 2 :c 3})))))

(deftest call-model-template-test
  (testing "Renders template and passes result to the LLM function"
    (let [captured-model (atom nil)
          captured-prompt (atom nil)
          mock-llm-fn (fn [model-name prompt-string]
                        (reset! captured-model model-name)
                        (reset! captured-prompt prompt-string)
                        {:content "mock response"})]

      (templater/call-model-template "test-model" "Summarize: {{text}}" {:text "This is a test."} mock-llm-fn)

      (is (= "test-model" @captured-model))
      (is (= "Summarize: This is a test." @captured-prompt)))))
