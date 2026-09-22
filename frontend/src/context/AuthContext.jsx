import React, { createContext, useContext, useEffect, useState } from "react";
import { api } from "@/lib/api";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(undefined); // undefined = loading, null = logged out
  const [booting, setBooting] = useState(true);

  useEffect(() => {
    const token = localStorage.getItem("pms_token");
    if (!token) {
      setUser(null);
      setBooting(false);
      return;
    }
    api
      .me()
      .then((res) => setUser(res.data))
      .catch(() => {
        localStorage.removeItem("pms_token");
        setUser(null);
      })
      .finally(() => setBooting(false));
  }, []);

  const login = async (username, password) => {
    const res = await api.login(username, password);
    localStorage.setItem("pms_token", res.data.token);
    setUser(res.data.user);
    return res.data.user;
  };

  const logout = async () => {
    try {
      await api.logout();
    } catch (_) {
      /* ignore */
    }
    localStorage.removeItem("pms_token");
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, booting, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);
