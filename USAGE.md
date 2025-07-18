# pcrit-llm Usage Guide

This guide explains how to use the `pcrit-llm` library to interact with Large Language Models and calculate their operational costs.

## Prerequisites

Before using the library, you must have a running [LiteLLM](https://github.com/BerriAI/litellm) proxy service and you must set the following environment variable:

```bash
export LITELLM_API_KEY="your-litellm-master-key"
```

The library includes a `pre-flight-checks` function that you should run at application startup to ensure the environment is configured correctly.

```clojure
(require '[pcrit.llm.core :as llm])

(when-not (llm/pre-flight-checks)
  (println "Aborting: LITELLM_API_KEY is not set.")
  (System/exit 1))```

## Basic Usage: Calling a Model

The primary function for interacting with an LLM is `pcrit.llm.core/call-model`. It takes a model name string and a prompt string.

It returns a map that will contain either a `:content` key on success or an `:error` key on failure. You should always check for the `:error` key first.

```clojure
(require '[pcrit.llm.core :as llm])

(let [response (llm/call-model "openai/gpt-4o-mini" "Translate 'hello world' to French.")]
  (if-let [error (:error response)]
    (println "LLM Call Failed:" error)
    (println "LLM Response:" (:content response))))

;; => LLM Response: "Bonjour le monde"
```

### Understanding the Response Metadata

On a successful call, the response map also contains a `:generation-metadata` key with rich information about the API call.

```clojure
(let [{:keys [content generation-metadata]} (llm/call-model "openai/gpt-4o-mini" "Why is the sky blue?")]
  (println "Content:" content)
  (clojure.pprint/pprint generation-metadata))
```

This will print:
```clojure
Content: The sky appears blue to the human eye because of a phenomenon called Rayleigh scattering...

{:model "openai/gpt-4o-mini",
 :provider :openai,
 :token-in 10,
 :token-out 152,
 :cost-usd-snapshot 0.0001062,
 :duration-ms 1258}
```

## Calculating Costs

The `pcrit.llm.costs` namespace allows you to calculate the cost of any hypothetical LLM call without needing to make a real API request. This is useful for planning and budgeting.

The `calculate-cost` function takes a model name, input tokens, and output tokens.

```clojure
(require '[pcrit.llm.costs :as costs])

(def model "openai/gpt-4o")
(def input-tokens 2500)
(def output-tokens 500)

(def estimated-cost (costs/calculate-cost model input-tokens output-tokens))
;; => 0.02

(println (format "The estimated cost for the call is $%.4f" estimated-cost))
;; => The estimated cost for the call is $0.0200
```

The full list of supported models and their prices is available in the public `costs/price-table` var.

## Using Templates

For convenience, the `pcrit.llm.templater` namespace provides a helper to render variables into a prompt string before calling the model. It uses [Selmer](httpshttps://github.com/yogthos/Selmer) syntax.

```clojure
(require '[pcrit.llm.templater :as templater])

(def prompt-template "Analyze the sentiment of the following text: {{text}}")
(def my-vars {:text "This new library is fantastic and easy to use!"})

(let [response (templater/call-model-template "openai/gpt-4o-mini" prompt-template my-vars)]
  (println (:content response)))

;; => "The sentiment of the text is positive."
```
