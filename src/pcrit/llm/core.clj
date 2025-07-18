(ns pcrit.llm.core
  (:require [clj-http.client :as http]
            [clojure.data.json :as json]
            [clojure.string :as str]
            [pcrit.llm.costs :as costs]))

(def ^:private config
  "Internal configuration for the pcrit-llm library."
  {:endpoint "http://localhost:8000/chat/completions"
   :default-timeout-ms 300000})

(def ^:private LITELLM_API_KEY (System/getenv "LITELLM_API_KEY"))

(defn pre-flight-checks
  "Checks for required environment variables."
  []
  (if (seq LITELLM_API_KEY)
    true
    (do
      (println "[pcrit-llm] ERROR: LITELLM_API_KEY environment variable not set.")
      false)))

(defn- -parse-provider [model-name]
  (keyword (first (str/split model-name #"/"))))

(defn parse-llm-response
  "Parses a clj-http response map to extract content and all generation metadata."
  [response model-name]
  (try
    (let [parsed-body (json/read-str (:body response) :key-fn keyword)
          usage       (:usage parsed-body)
          token-in    (or (:prompt_tokens usage) 0)
          token-out   (or (:completion_tokens usage) 0)
          content     (-> parsed-body :choices first :message :content)]
      (if content
        {:content content
         :generation-metadata {:model             model-name
                               :provider          (-parse-provider model-name)
                               :token-in          token-in
                               :token-out         token-out
                               :cost-usd-snapshot (costs/calculate-cost model-name token-in token-out)}}
        (do
          (println "[pcrit-llm] ERROR: Could not extract content from LLM response for" model-name ". Body:" (:body response))
          {:error (str "No content in LLM response: " (pr-str parsed-body))})))
    (catch Exception e
      (println "[pcrit-llm] ERROR: Failed to parse LLM JSON response for" model-name ". Error:" (.getMessage e) ". Body:" (:body response))
      {:error (str "Malformed JSON from LLM: " (.getMessage e))})))

(defn call-model
  "Calls an LLM with a model name and a prompt string.
  Returns a map with the content and detailed generation metadata, or an error map."
  [model-name prompt-string & {:keys [timeout post-fn]
                               :or {timeout (:default-timeout-ms config)
                                    post-fn http/post}}]
  (let [endpoint (:endpoint config)]
    (println "[pcrit-llm] INFO: Calling LLM:" model-name "via" endpoint)
    (try
      (let [request-body {:model model-name
                          :messages [{:role "user" :content prompt-string}]}
            headers {"Authorization" (str "Bearer " LITELLM_API_KEY)}
            start-time (System/currentTimeMillis)
            response (post-fn endpoint
                              {:body (json/write-str request-body)
                               :content-type :json
                               :accept :json
                               :headers headers
                               :throw-exceptions false
                               :socket-timeout timeout
                               :connection-timeout timeout})
            duration-ms (- (System/currentTimeMillis) start-time)]
        (if (= 200 (:status response))
          (let [{:keys [content generation-metadata error]} (parse-llm-response response model-name)]
            (if error
              {:error error}
              {:content content
               :generation-metadata (assoc generation-metadata :duration-ms duration-ms)}))
          (do
            (println "[pcrit-llm] ERROR: LLM call to" model-name "failed with status" (:status response) ". Body:" (:body response))
            {:error (str "LLM API Error: " (:status response) " " (:body response))})))
      (catch Exception e
        (println "[pcrit-llm] ERROR: Exception during LLM call to" model-name ". Error:" (.getMessage e))
        {:error (str "Network or client exception: " (.getMessage e))}))))
