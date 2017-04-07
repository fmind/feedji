#!/usr/bin/env boot

(set-env! :dependencies '[[org.clojure/data.xml "0.0.8"]
                          [com.rometools/rome "1.7.1"]
                          [http-kit "2.2.0"]])

(require '[boot.cli :refer [defclifn]]
         '[org.httpkit.client :as H]
         '[clojure.data.xml :as X]
         '[clojure.java.io :as io])

(import (com.rometools.rome.feed.synd SyndFeed)
        (com.rometools.rome.io SyndFeedInput XmlReader))

(defn feed? [element]
  (-> element :attrs :xmlUrl))

(defn print-err [& more]
  (.println *err* (apply str more)))

(defn fetch-entries! [url]
  (try
    ;; only http-kit can follow redirects
    (some->> @(H/get url {:as :stream}) :body io/reader
              (.build (SyndFeedInput.)) .getEntries seq)
  (catch Exception e
    (print-err "ERROR: " e " >> " url))))

(defn analyze-feed [feed]
  (let [attrs (:attrs feed)
        {:keys [title xmlUrl]} attrs
        entries (fetch-entries! xmlUrl)]
    (println title (count entries))
    [title (count entries)]))

(defn analyze-opml [opml]
  (->> (xml-seq opml)
       (filter feed?)
       (map analyze-feed)))

(defn format-output [output]
  output)

(defclifn
  -main "OPML->statistics."
  [o out FILE file "output file."]
  (let [in (when-let [in (first *args*)] (io/as-file in))]
    (with-open [r (io/reader (or in *in*))]
      (with-open [w (io/writer (or out *out*))]
        (->> (slurp r)
             X/parse-str
             analyze-opml
             format-output
             (spit w))))))

