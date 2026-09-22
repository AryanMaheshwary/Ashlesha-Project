import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Cross, ShieldCheck } from "lucide-react";
import { toast } from "sonner";
import { useAuth } from "@/context/AuthContext";
import { apiError } from "@/lib/api";
import { Btn, Field, TextInput } from "@/components/common";

const DEMO = [
  { role: "Admin", username: "admin", password: "admin123" },
  { role: "Pharmacist", username: "pharma", password: "pharma123" },
  { role: "Cashier", username: "cashier", password: "cashier123" },
];

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const user = await login(username, password);
      toast.success(`Welcome back, ${user.fullName}`);
      navigate(user.role === "CASHIER" ? "/billing" : "/dashboard");
    } catch (err) {
      toast.error(apiError(err));
    } finally {
      setLoading(false);
    }
  };

  const quickFill = (d) => {
    setUsername(d.username);
    setPassword(d.password);
  };

  return (
    <div className="min-h-screen flex">
      {/* Left brand panel */}
      <div className="hidden lg:flex lg:w-1/2 bg-[#0F172A] relative overflow-hidden flex-col justify-between p-12">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-lg bg-[#0D9488] flex items-center justify-center">
            <Cross className="w-6 h-6 text-white" />
          </div>
          <span className="font-heading text-xl font-bold text-white">MediTrack</span>
        </div>
        <div className="relative z-10">
          <h1 className="font-heading text-4xl font-extrabold text-white leading-tight">
            Pharmacy Management,<br /> precise and safe.
          </h1>
          <p className="text-slate-400 mt-4 max-w-md">
            Batch-level stock, FIFO expiry billing, prescription safety checks and
            live sales reporting — powered by a Java + MySQL backend.
          </p>
          <div className="flex items-center gap-2 mt-8 text-teal-300 text-sm">
            <ShieldCheck className="w-4 h-4" /> Role-based access · Transactional billing
          </div>
        </div>
        <div
          className="absolute -right-24 -bottom-24 w-96 h-96 rounded-full bg-[#0D9488]/20 blur-3xl"
          aria-hidden
        />
      </div>

      {/* Right form */}
      <div className="flex-1 flex items-center justify-center p-6 bg-[#F8FAFC]">
        <div className="w-full max-w-md">
          <div className="lg:hidden flex items-center gap-3 mb-8 justify-center">
            <div className="w-10 h-10 rounded-lg bg-[#0D9488] flex items-center justify-center">
              <Cross className="w-6 h-6 text-white" />
            </div>
            <span className="font-heading text-xl font-bold">MediTrack</span>
          </div>

          <h2 className="font-heading text-2xl font-bold text-slate-900">Sign in</h2>
          <p className="text-sm text-slate-500 mt-1 mb-6">Access your pharmacy workspace.</p>

          <form onSubmit={submit} className="space-y-4">
            <Field label="Username">
              <TextInput
                data-testid="login-username-input"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="e.g. admin"
                autoFocus
              />
            </Field>
            <Field label="Password">
              <TextInput
                data-testid="login-password-input"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
              />
            </Field>
            <Btn
              type="submit"
              data-testid="login-submit-button"
              className="w-full"
              disabled={loading}
            >
              {loading ? "Signing in..." : "Sign in"}
            </Btn>
          </form>

          <div className="mt-8">
            <div className="text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
              Demo accounts
            </div>
            <div className="grid grid-cols-3 gap-2">
              {DEMO.map((d) => (
                <button
                  key={d.username}
                  onClick={() => quickFill(d)}
                  data-testid={`demo-${d.username}`}
                  className="text-left px-3 py-2 rounded-lg border border-slate-200 bg-white hover:border-[#0D9488] transition-colors"
                >
                  <div className="text-xs font-semibold text-slate-800">{d.role}</div>
                  <div className="text-[11px] text-slate-400 font-mono-data">{d.username}</div>
                </button>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
