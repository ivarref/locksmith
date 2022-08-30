(ns com.github.ivarref.locksmith
  (:import (java.util.concurrent TimeUnit)
           (okhttp3.tls HeldCertificate HeldCertificate$Builder)))

(defn gen-certs [{:keys [duration-days]
                  :or   {duration-days 365}}]
  (let [^HeldCertificate rootCertificate (-> (HeldCertificate$Builder.)
                                             (.certificateAuthority 0)
                                             (.duration duration-days TimeUnit/DAYS)
                                             (.build))
        ^HeldCertificate serverCertificate (-> (HeldCertificate$Builder.)
                                               (.signedBy rootCertificate)
                                               (.duration duration-days TimeUnit/DAYS)
                                               (.build))
        ^HeldCertificate client (-> (HeldCertificate$Builder.)
                                    (.signedBy rootCertificate)
                                    (.duration duration-days TimeUnit/DAYS)
                                    (.build))]
    {:ca-cert     (.certificatePem rootCertificate)
     :ca-key      (.privateKeyPkcs8Pem rootCertificate)
     :server-cert (.certificatePem serverCertificate)
     :server-key  (.privateKeyPkcs8Pem serverCertificate)
     :client-cert (.certificatePem client)
     :client-key  (.privateKeyPkcs8Pem client)}))

(defn write-certs [opts]
  (let [{:keys [ca-cert
                server-cert server-key
                client-cert client-key]}
        (gen-certs opts)]
    (spit "server.keys" (str ca-cert server-cert server-key))
    (println "Wrote server.keys")

    (spit "client.keys" (str ca-cert client-cert client-key))
    (println "Wrote client.keys")))
