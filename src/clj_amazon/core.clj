;; Copyright (C) 2011, Eduardo Julián. All rights reserved.
;;
;; The use and distribution terms for this software are covered by the 
;; Eclipse Public License 1.0
;; (http://opensource.org/licenses/eclipse-1.0.php) which can be found
;; in the file epl-v10.html at the root of this distribution.
;;
;; By using this software in any fashion, you are agreeing to be bound
;; by the terms of this license.
;;
;; You must not remove this notice, or any other, from this software.

(ns clj-amazon.core
  "The core functionality shared by other namespaces."
  (:import (java.io ByteArrayInputStream))
  (:require [clj-http.client :as http]
            [clojure.xml :as xml]))

; The following is a Clojure version of Amazon's SignedRequestsHelper class + some modifications.
(def +utf-8+ "UTF-8")
(def +hmac-sha256+ "HmacSHA256")
(def +request-uri+ "/onca/xml")
(def +request-method+ "GET")

(def +service+ "AWSECommerceService")
(def +service-version+ "2011-08-01" ;"2009-03-31"
  )
(def +endpoint+ "ecs.amazonaws.com")

(defn percent-encode-rfc-3986 [s]
  (-> (java.net.URLEncoder/encode (str s) +utf-8+)
    (.replace "+" "%20")
    (.replace "*" "%2A")
    (.replace "%7E" "~")))

(defn timestamp []
  (-> (doto (java.text.SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss'.000Z'")
        (.setTimeZone (java.util.TimeZone/getTimeZone "GMT")))
    (.format (.getTime (java.util.Calendar/getInstance)))))

(defn canonicalize [sorted-map]
  (if (empty? sorted-map)
    ""
    (->> sorted-map
      (map (fn [[k v]] (if v (str (percent-encode-rfc-3986 k) "=" (percent-encode-rfc-3986 v)))))
      (filter (comp not nil?))
      (interpose "&")
      (apply str)
      ;((fn [_] (prn _) _))
      )
    ))

(defprotocol ISignedRequestsHelper
  (sign [self params])
  (hmac [self string]))

; Forgive the weird code. I just like writing point-free kind of code.
(defrecord SignedRequestsHelper [endpoint access-key secret-key secret-key-spec mac]
  ISignedRequestsHelper
  (sign [self params]
    (let [query-str (-> params
                      (assoc "AWSAccessKeyId" access-key, "Timestamp" (timestamp))
                      ;java.util.TreeMap.
                      canonicalize)]
      (->> query-str
        (str +request-method+ "\n" endpoint "\n" +request-uri+ "\n")
        (.hmac self)
        percent-encode-rfc-3986
        (str "http://" endpoint +request-uri+ "?" query-str "&Signature="))
      ))
  (hmac [self string]
    (-> string (.getBytes +utf-8+)
      (->> (.doFinal mac)
        (.encode (org.apache.commons.codec.binary.Base64. 76 (byte-array 0))))
      String.)))

(defn signed-request-helper "Try not to use this directly. Better use it through with-signer."
  [access-key secret-key]
  (let [secret-key-spec (-> secret-key (.getBytes +utf-8+) (javax.crypto.spec.SecretKeySpec. +hmac-sha256+))
        mac (javax.crypto.Mac/getInstance +hmac-sha256+)]
    (.init mac secret-key-spec)
    (SignedRequestsHelper. +endpoint+ access-key secret-key
                           secret-key-spec mac)))

; The following are the basic utils for Amazon's APIs.
(def ^:dynamic *signer*)

(defn- parse-xml [xml] (xml/parse (ByteArrayInputStream. (.getBytes xml "UTF-8"))))
(defn fetch-url [url] (-> url http/get :body parse-xml))

(defn encode-url [url] (if url (java.net.URLEncoder/encode url +utf-8+)))

(defn assoc+
  ([m k v]
   (let [item (get m k)]
     (if k
       (cond (nil? item) (assoc m k v)
             (vector? item) (assoc m k (conj item v))
             :else (assoc m k [item v]))
       m)))
  ([m k v & kvs] (apply assoc+ (assoc+ m k v) kvs)))

(defn _bool->str [bool] (if bool "True" "False"))

(defn _str->sym [string]
  (-> (reduce #(if (Character/isUpperCase %2) (str %1 "-" (Character/toLowerCase %2)) (str %1 %2)) "" string)
    (.substring 1) symbol ))

(defn _extract-strs [strs] (map #(if (list? %) (second %) %) strs))

(defn _extract-vars [strs] (map _str->sym (_extract-strs strs)))

(defmacro with-signer
  "Evaluates the given forms with the given [\"Access Key\", \"Secret Key\"] pair."
  [signer-params & body]
  `(binding [*signer* (signed-request-helper ~@signer-params)]
     ~@body))
