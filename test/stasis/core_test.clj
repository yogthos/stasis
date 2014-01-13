(ns stasis.core-test
  (:require [stasis.core :refer :all]
            [midje.sweet :refer :all]
            [clojure.java.io :as io]
            [test-with-files.core :refer [with-tmp-dir tmp-dir]]))

(fact
 "Stasis creates a Ring handler to serve your pages."

 (let [app (serve-pages {"/page.html" (fn [req] {:body "The page contents"})})]

   (fact (app {:uri "/page.html"})
         => {:status 200
             :body "The page contents"
             :headers {"Content-Type" "text/html"}})

   (fact (app {:uri "/missing.html"})
         => {:status 404
             :body "<h1>Page not found</h1>"
             :headers {"Content-Type" "text/html"}})))
(fact
 "If you use paths without .html, it serves them as directories with
  an index.html."

 (let [app (serve-pages {"/page/" (fn [req] {:body (str "I'm serving " (:uri req))})})]

   (fact (:body (app {:uri "/page/index.html"})) => "I'm serving /page/index.html")
   (fact (:body (app {:uri "/page/"})) => "I'm serving /page/index.html")
   (fact (:body (app {:uri "/page"})) => "I'm serving /page/index.html")))

(fact
 "You can serve other types of assets too."

 (let [app (serve-pages {"/page-details.js" (fn [req] {:body (str "alert('" (:uri req) "');")})})]

   (fact (app {:uri "/page-details.js"}) => {:status 200
                                             :body "alert('/page-details.js');"})))

(fact
 "Stasis exports pages to your directory of choice."

 (with-tmp-dir
   (export-pages {"/page/index.html" (fn [req] {:body "The contents"})}
                 tmp-dir {})
   (slurp (str tmp-dir "/page/index.html")) => "The contents"))

(fact
 "Stasis adds the :uri the 'request' for exported pages too."

 (with-tmp-dir
   (export-pages {"/page" (fn [req] {:body (str "I'm serving " (:uri req))})}
                 tmp-dir {})
   (slurp (str tmp-dir "/page/index.html")) => "I'm serving /page/index.html"))

(fact
 "You can add more information to the 'request' if you want. Like
  configuration options, or optimus assets."

 (with-tmp-dir
   (export-pages {"/page" (fn [req] {:body (str "I got " (:conf req))})}
                 tmp-dir {:conf "served"})
   (slurp (str tmp-dir "/page/index.html")) => "I got served"))

(with-tmp-dir
  (export-pages {"/folder/page.html" (fn [req] {:body "Contents"})} tmp-dir {})

  (fact
   "You can't accidentaly delete a file with delete-directory!"

   (delete-directory! (str tmp-dir "/folder/page.html"))
   => (throws Exception (str tmp-dir "/folder/page.html is not a directory.")))

  (fact
   "But it's really easy emptying an entire folder of files. Be careful."

   (delete-directory! (str tmp-dir "/folder"))

   (io/as-file (str tmp-dir "/folder/page.html")) => #(not (.exists %))
   (io/as-file (str tmp-dir "/folder")) => #(not (.exists %)))

  (fact
   "Deleting non-existing folders is a-o-k. It's all about the idempotence, baby."

   (delete-directory! (str tmp-dir "/missing"))))
