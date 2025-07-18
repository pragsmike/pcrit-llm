(ns pcrit.llm.costs
  "Provides LLM cost calculation logic based on a static price table.")

(def price-table
  "A map of model names to their cost per 1000 tokens for input and output.
  Source: Public pricing pages as of July 2025."
  {;; --- OpenAI Models ---
   "openai/gpt-4o"          {:in-per-1k 0.005,   :out-per-1k 0.015}
   "openai/gpt-4o-mini"     {:in-per-1k 0.00015, :out-per-1k 0.0006}
   "openai/gpt-4-turbo"     {:in-per-1k 0.01,    :out-per-1k 0.03}
   "openai/gpt-3.5-turbo"   {:in-per-1k 0.0005,  :out-per-1k 0.0015}

   ;; --- Anthropic Models ---
   "anthropic/claude-3-opus"  {:in-per-1k 0.015,   :out-per-1k 0.075}
   "anthropic/claude-3-sonnet" {:in-per-1k 0.003,   :out-per-1k 0.015}
   "anthropic/claude-3-haiku" {:in-per-1k 0.00025, :out-per-1k 0.00125}
   "anthropic/claude-3.7-sonnet"  {:in-per-1k 0.003, :out-per-1k 0.015}

   ;; --- Google Gemini Models ---
   "google/gemini-1.5-pro"    {:in-per-1k 0.0035,  :out-per-1k 0.0105}
   "google/gemini-1.5-flash"  {:in-per-1k 0.00035, :out-per-1k 0.00105}

   ;; --- Perplexity Models ---
   "perplexity/llama-3-sonar-small-32k-online" {:in-per-1k 0.0002, :out-per-1k 0.0002}
   "perplexity/llama-3-sonar-large-32k-online" {:in-per-1k 0.001,  :out-per-1k 0.001}

   ;; --- Self-Hosted Ollama Models (cost is 0) ---
   "ollama/mistral"           {:in-per-1k 0.0, :out-per-1k 0.0}
   "ollama/qwen3:32b"         {:in-per-1k 0.0, :out-per-1k 0.0}
   "ollama/qwen3:30b"         {:in-per-1k 0.0, :out-per-1k 0.0}
   "ollama/qwen3:14b"         {:in-per-1k 0.0, :out-per-1k 0.0}
   "ollama/mistral-nemo:12b"  {:in-per-1k 0.0, :out-per-1k 0.0}
   "ollama/gemma3:12b"        {:in-per-1k 0.0, :out-per-1k 0.0}
   "ollama/qwen3:8b"          {:in-per-1k 0.0, :out-per-1k 0.0}
   "ollama/granite3.3:8b"     {:in-per-1k 0.0, :out-per-1k 0.0}
   "ollama/qwen3:4b"          {:in-per-1k 0.0, :out-per-1k 0.0}
   "ollama/qwen3:1.7b"        {:in-per-1k 0.0, :out-per-1k 0.0}
   "ollama/granite3.3:2b"     {:in-per-1k 0.0, :out-per-1k 0.0}
   "ollama/gemma3:1b"         {:in-per-1k 0.0, :out-per-1k 0.0}})

(defn calculate-cost
  "Calculates the monetary cost for an LLM call given the model and token counts."
  [model-name token-in token-out]
  (let [token-in'  (or token-in 0)
        token-out' (or token-out 0)]
    (if-let [pricing (get price-table model-name)]
      (+ (* (/ (double token-in') 1000.0) (:in-per-1k pricing))
         (* (/ (double token-out') 1000.0) (:out-per-1k pricing)))
      0.0)))
