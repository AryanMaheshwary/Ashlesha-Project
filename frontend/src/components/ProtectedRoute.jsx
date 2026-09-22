import React from "react";
import { Navigate } from "react-router-dom";
import { useAuth } from "@/context/AuthContext";
import { Spinner } from "@/components/common";

export default function ProtectedRoute({ children, roles }) {
  const { user, booting } = useAuth();

  if (booting || user === undefined) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <Spinner label="Checking session..." />
      </div>
    );
  }
  if (!user) return <Navigate to="/login" replace />;
  if (roles && !roles.includes(user.role)) return <Navigate to="/billing" replace />;
  return children;
}
