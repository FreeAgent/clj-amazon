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

(ns clj-amazon.product-advertising
  "This is a small Clojure binding for the Amazon Product Advertising API. You can find more information about the API here: http://docs.amazonwebservices.com/AWSECommerceService/latest/DG/index.html?Welcome.html"
  (:use clj-amazon.core)
  (:require [clojure.walk :as walk]))

(defn- parse-results [xml]
  ;(prn xml)
  (case (:tag xml)
    ; Stuff to omit
    :OperationRequest [nil nil]
    :Request [nil nil]
    ; Containers
    :BrowseNodeLookupResponse (parse-results (second (:content xml)))
    :ItemSearchResponse (parse-results (second (:content xml)))
    :BrowseNodes (reduce #(apply assoc+ %1 (parse-results %2)) {} (:content xml))
    :Items (reduce #(apply assoc+ %1 (parse-results %2)) {:items []} (:content xml))
    ; Stuff to use
    :Actor [:actor (first (:content xml))]
    :Ancestors [:ancestors (reduce #(apply assoc+ %1 (parse-results %2)) {} (:content xml))]
    :ASIN [:asin (first (:content xml))]
    :Author [:author (first (:content xml))]
    :BrowseNode (reduce #(apply assoc+ %1 (parse-results %2)) {} (:content xml))
    :BrowseNodeId [:browser-node-id (read-string (first (:content xml)))]
    :Children [:children (reduce #(apply assoc+ %1 (parse-results %2)) {} (:content xml))]
    :DetailPageURL [:detail-page-url (first (:content xml))]
    :Item [:items (reduce #(apply assoc+ %1 (parse-results %2)) {} (:content xml))]
    :ItemAttributes [:item-atributes (reduce #(apply assoc+ %1 (parse-results %2)) {} (:content xml))]
    :ItemLinks [:item-links (vec (map parse-results (:content xml)))]
    :ItemLink {:description (-> xml :content first :content first), :url (-> xml :content second :content first)}
    :Manufacturer [:manufacturer (first (:content xml))]
    :Name [:name (first (:content xml))]
    :ProductGroup [:product-group (first (:content xml))]
    :NewReleases [:new-releases (map parse-results (:content xml))]
    :NewRelease (reduce #(apply assoc+ %1 (parse-results %2)) {} (:content xml))
    :Title [:title (first (:content xml))]
    :TopItem (reduce #(apply assoc+ %1 (parse-results %2)) {} (:content xml))
    :TopItemSet [:top-item-set (reduce #(apply assoc+ %1 (parse-results %2)) {} (:content xml))]
    :TotalPages [:total-pages (read-string (first (:content xml)))]
    :TotalResults [:total-results (read-string (first (:content xml)))]
    :Type [:type (first (:content xml))]
    [nil nil] ; In case some weird tag appears, ignore it for now.
    ))

; Imagine that this macro is a VERY specialized do-template
(defmacro ^:private make-fns [& specifics] 
  (let [standard-fields '(response-group subscription-id associate-tag merchant-id)]
    `(do ~@(for [[operation specific-appends] (partition 2 specifics)]
             (let [strs (_extract-strs specific-appends),
                   vars (_extract-vars specific-appends),
                   susts (apply hash-map (interleave strs vars)),
                   mvars (walk/prewalk-replace susts specific-appends)]
               `(defn ~(_str->sym operation) "" [& {:keys ~(vec (concat standard-fields vars))}]
                  (->> (sorted-map "Service" +service+, "Version" +service-version+, "Operation" ~operation,
                                   "ResponseGroup" ~'response-group, "SubscriptionId" ~'subscription-id,
                                   "AssociateTag" ~'associate-tag, "MerchantId" ~'merchant-id,
                                   ~@(interleave strs mvars))
                    (.sign *signer*) fetch-url parse-results
                    )))
             ))))

(make-fns
  "ItemLookup" ; item-lookup
  ["ItemId" "SearchIndex" "Condition" "IdType" (_bool->str "IncludeReviewsSummary") "OfferPage" "VariationPage"
   "RelatedItemPage" "RelationshipType" "ReviewPage" "ReviewSort" "TagPage" "TagsPerPage" "TagSort" "TruncateReviewsAt"]
  ;;;;;;;;;;;;;;;;;;
  "ItemSearch" ; item-search
  ["Actor" "Artist" "AudienceRating" "Author" "Availability" "Brand" "BrowseNode" "City" "Composer" "Condition" "Conductor"
   "Director" (_bool->str "IncludeReviewsSummary") "ItemPage" (encode-url "Keywords") "Manufacturer" "MaximumPrice" "MinimumPrice"
   "Neighborhood" "Orchestra" "PostalCode" "Power" "Publisher" "RelatedItemPage" "RelationshipType" "ReviewSort" "SearchIndex"
   "Sort" "TagPage" "TagsPerPage" "TagSort" "TextStream" "Title" "TruncateReviewsAt" "VariationPage"]
  ;;;;;;;;;;;;;;;;;;
  "BrowseNodeLookup" ["BrowseNodeId"] ; browse-node-lookup
  ;;;;;;;;;;;;;;;;;;
  "CartAdd" ["ASIN" "CartId" "HMAC" "Item" "Items" "OfferListingId" "Quantity"] ; cart-add
  ;;;;;;;;;;;;;;;;;;
  "CartClear" ["CartId" "HMAC"] ; cart-clear
  ;;;;;;;;;;;;;;;;;;
  "CartCreate" ["ASIN" "Item" "Items" "ListItemId" "OfferListingId" "Quantity"] ; cart-create
  ;;;;;;;;;;;;;;;;;;
  "CartGet" ["CartId" "CartItemId" "HMAC"] ; cart-get
  ;;;;;;;;;;;;;;;;;;
  "CartModify" ["Action" "CartId" "CartItemId" "HMAC" "Item" "Items" "Quantity"] ; cart-modify
  ;;;;;;;;;;;;;;;;;;
  "SellerListingLookup" ["Id" "IdType" "SellerId"] ; seller-listing-lookup
  ;;;;;;;;;;;;;;;;;;
  "SellerListingSearch" ["ListingPage" "OfferStatus" "SellerId" "Sort" "Title"] ; seller-listing-search
  ;;;;;;;;;;;;;;;;;;
  "SellerLookup" ["FeedbackPage" "SellerId"] ; seller-lookup
  ;;;;;;;;;;;;;;;;;;
  "SimilarityLookup" ["Condition" "ItemId" "SimilarityType"] ; similarity-lookup
  )
