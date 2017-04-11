#!/usr/bin/env boot

(set-env! :dependencies '[[org.clojure/data.xml "0.0.8"]
                          [com.rometools/rome "1.7.1"]
                          [clj-time "0.13.0"]
                          [http-kit "2.2.0"]])

(require '[boot.cli :refer [defclifn]]
         '[org.httpkit.client :as Http]
         '[clojure.data.xml :as Xml]
         '[clj-time.core :as Time]
         '[clj-time.coerce :as TC]
         '[clojure.java.io :as io])

(import (com.rometools.rome.feed.synd SyndFeed)
        (com.rometools.rome.feed.synd SyndEntry)
        (com.rometools.rome.io SyndFeedInput XmlReader))

(defn feed? [element]
  (get-in element [:attrs :xmlUrl]))

(defn print-err! [& more]
  (.println *err* (apply str more)))

(defn fetch-entries! [url]
  (try
    ;; only http-kit can follow http redirections
    (some->> @(Http/get url {:as :stream}) :body io/reader
              (.build (SyndFeedInput.)) .getEntries seq)
  (catch Exception e
    (print-err! "ERROR: " e " >> " url))))

(def parse-opml Xml/parse-str)

(defn entries->stats [entries]
  (if (or (nil? entries) (zero? (count entries))) nil
      (let [dates (map #(TC/from-date (.getPublishedDate %)) entries)
            from (last dates), to (first dates)
            interval (Time/interval from to)
            hours (Time/in-hours interval)
            hours (if (zero? hours) 1 hours)
            total (count entries)]
        {:entries total
         :in-hours hours
         :per-hour (float (/ total hours))})))

(defn analyze-feed [feed]
  (let [attrs (:attrs feed)
        {:keys [title xmlUrl]} attrs
        entries (fetch-entries! xmlUrl)
        stats (entries->stats entries)]
    [title stats]))

(defn analyze-opml [xml]
  (->> (xml-seq xml)
       (filterv feed?)
       (pmap analyze-feed)))

(defn format-output [analysis]
  (->> analysis (into {}) pr-str))

(def process-opml (comp analyze-opml parse-opml))

(defclifn -main "Compute statistics from an OPML file." []
  (->> (slurp *in*) process-opml format-output (spit *out*)))
