(ns svc-herald.auth)

(defprotocol Auth
  (wrap-auth-cookie [this app])
  (get-current-user-guid [this request])
  (get-user-info [this guid])
  (authenticate
    [this handler]
    [this handler anonymous-action]))

(deftype NoAuth []
  Auth
  (wrap-auth-cookie [this app] app)
  (get-current-user-guid [this request] nil)
  (get-user-info [this guid] nil)
  (authenticate [this handler] handler)
  (authenticate [this handler anonymous-action] handler))

(defn no-auth [] (new NoAuth))

