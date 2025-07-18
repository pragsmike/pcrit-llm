(ns pcrit.llm.costs-test
  (:require [clojure.test :refer :all]
            [pcrit.llm.costs :as costs]))

(def ^:private test-price-table
  {"test/expensive-model" {:in-per-1k 0.10, :out-per-1k 0.50}
   "test/free-model"      {:in-per-1k 0.0,  :out-per-1k 0.0}})

(deftest calculate-cost-test
  (testing "Cost calculation with a mock price table"
    (with-redefs [costs/price-table test-price-table]

      (testing "calculates cost for a known, priced model"
        ;; (200/1000 * 0.10) + (100/1000 * 0.50) = 0.02 + 0.05 = 0.07
        (is (< (Math/abs (- 0.07 (costs/calculate-cost "test/expensive-model" 200 100))) 1e-9)))

      (testing "returns 0.0 for a known free model"
        (is (zero? (costs/calculate-cost "test/free-model" 1000 1000))))

      (testing "returns 0.0 for an unknown model"
        (is (zero? (costs/calculate-cost "unknown/model" 1000 1000))))

      (testing "handles nil token counts gracefully"
        (is (zero? (costs/calculate-cost "test/expensive-model" nil nil))))

      (testing "calculates cost with only input tokens"
        ;; (500/1000 * 0.10) = 0.05
        (is (< (Math/abs (- 0.05 (costs/calculate-cost "test/expensive-model" 500 nil))) 1e-9)))

      (testing "calculates cost with only output tokens"
        ;; (50/1000 * 0.50) = 0.025
        (is (< (Math/abs (- 0.025 (costs/calculate-cost "test/expensive-model" nil 50))) 1e-9))))))
