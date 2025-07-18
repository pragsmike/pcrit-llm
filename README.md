# pcrit-llm

A simple, focused Clojure library for interacting with LLM providers via a [LiteLLM](https://github.com/BerriAI/litellm)-compatible API proxy.

`pcrit-llm` provides two core functions:
1.  A straightforward way to call different LLM models for text completion.
2.  A utility to calculate the monetary cost of an LLM call based on model name and token counts.

This library was extracted from the [PromptCritical](https://github.com/pragsmike/promptcritical) project.

## Installation

Add the following to your `deps.edn` file:

```clojure
pcrit/pcrit-llm {:git/url "https://github.com/<your-username>/pcrit-llm" :git/sha "<commit-sha>"}```
*(Once published, this will change to a standard Maven dependency from Clojars.)*

## Prerequisites

You must have a LiteLLM instance running and accessible. The library requires one environment variable to be set:

*   `LITELLM_API_KEY`: The API key for your LiteLLM proxy.

The default endpoint is `http://localhost:8000/chat/completions`.

## Usage

### Calling an LLM

The main function is `pcrit.llm.core/call-model`. It takes a model name and a prompt string and returns a map containing either the `:content` or an `:error`.

```clojure
(require '[pcrit.llm.core :as llm])

;; Check for API key before starting
(when (llm/pre-flight-checks)

  ;; Simple API call
  (let [response (llm/call-model "openai/gpt-4o-mini" "Translate 'hello' to French.")]
    (if-let [error (:error response)]
      (println "Error:" error)
      (println "Response:" (:content response))))

  ;; The full response includes detailed metadata
  (let [{:keys [content generation-metadata]} (llm/call-model "openai/gpt-4o-mini" "Why is the sky blue?")]
    (println "Content:" content)
    (clojure.pprint/pprint generation-metadata)))
```

Example `generation-metadata` output:
```clojure
{:model "openai/gpt-4o-mini",
 :provider :openai,
 :token-in 10,
 :token-out 152,
 :cost-usd-snapshot 0.0001062,
 :duration-ms 1258}
```

### Calculating Cost

You can calculate the cost of any hypothetical LLM call using `pcrit.llm.costs/calculate-cost`.

```clojure
(require '[pcrit.llm.costs :as costs])

(def model "openai/gpt-4o")
(def input-tokens 2500)
(def output-tokens 500)

(def a-cost (costs/calculate-cost model input-tokens output-tokens))
;; => 0.02

(println (format "The estimated cost is $%.4f" a-cost))
;; => The estimated cost is $0.0200
```

### Using Templates

A helper namespace, `pcrit.llm.templater`, is provided for rendering variables into a prompt string before making an API call.

```clojure
(require '[pcrit.llm.templater :as templater])

(def prompt-template "Analyze the sentiment of the following text: {{text}}")
(def my-vars {:text "I love this new library!"})

(def response (templater/call-model-template "openai/gpt-4o-mini" prompt-template my-vars))

(println (:content response))
;; => "The sentiment of the text is positive."
```

## Development

To run the tests for this library, use the `:test` alias:

```bash
clj -M:test
```
