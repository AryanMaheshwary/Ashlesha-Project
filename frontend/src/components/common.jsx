import React from "react";

export function cx(...parts) {
  return parts.filter(Boolean).join(" ");
}

export function Card({ className, children, ...rest }) {
  return (
    <div
      className={cx(
        "bg-white border border-slate-200 rounded-lg shadow-sm",
        className
      )}
      {...rest}
    >
      {children}
    </div>
  );
}

export function Btn({ variant = "primary", className, children, ...rest }) {
  const styles = {
    primary:
      "bg-[#0D9488] hover:bg-[#0F766E] text-white shadow-sm",
    outline:
      "bg-white border border-slate-300 hover:border-[#0D9488] hover:text-[#0F766E] text-slate-700",
    danger: "bg-[#DC2626] hover:bg-[#b91c1c] text-white",
    ghost: "bg-transparent hover:bg-slate-100 text-slate-700",
    subtle: "bg-slate-100 hover:bg-slate-200 text-slate-800",
  };
  return (
    <button
      className={cx(
        "inline-flex items-center justify-center gap-2 px-4 py-2 rounded-lg text-sm font-semibold",
        "transition-colors duration-150 disabled:opacity-50 disabled:cursor-not-allowed",
        styles[variant],
        className
      )}
      {...rest}
    >
      {children}
    </button>
  );
}

export function Field({ label, children, hint }) {
  return (
    <label className="block">
      <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">
        {label}
      </span>
      <div className="mt-1.5">{children}</div>
      {hint && <span className="text-xs text-slate-400 mt-1 block">{hint}</span>}
    </label>
  );
}

export function TextInput({ className, ...rest }) {
  return (
    <input
      className={cx(
        "w-full px-3 py-2 rounded-lg border border-slate-300 bg-white text-sm",
        "focus:outline-none focus:border-[#0D9488] focus:ring-2 focus:ring-[#0D9488]/20",
        "placeholder:text-slate-400",
        className
      )}
      {...rest}
    />
  );
}

export function Select({ className, children, ...rest }) {
  return (
    <select
      className={cx(
        "w-full px-3 py-2 rounded-lg border border-slate-300 bg-white text-sm",
        "focus:outline-none focus:border-[#0D9488] focus:ring-2 focus:ring-[#0D9488]/20",
        className
      )}
      {...rest}
    >
      {children}
    </select>
  );
}

export function RoleBadge({ role }) {
  const map = {
    ADMIN: "bg-purple-100 text-purple-800 border-purple-200",
    PHARMACIST: "bg-teal-100 text-teal-800 border-teal-200",
    CASHIER: "bg-blue-100 text-blue-800 border-blue-200",
  };
  return (
    <span
      data-testid="role-badge"
      className={cx(
        "inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold border shadow-sm",
        map[role] || "bg-slate-100 text-slate-700 border-slate-200"
      )}
    >
      {role}
    </span>
  );
}

export function Modal({ open, onClose, title, children, wide }) {
  if (!open) return null;
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div
        className="absolute inset-0 bg-slate-900/60"
        onClick={onClose}
        data-testid="modal-overlay"
      />
      <div
        className={cx(
          "relative bg-white rounded-lg shadow-2xl border border-slate-200 w-full max-h-[90vh] overflow-y-auto pms-scroll",
          wide ? "max-w-3xl" : "max-w-lg"
        )}
      >
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100 sticky top-0 bg-white">
          <h3 className="font-heading text-lg font-semibold text-slate-900">{title}</h3>
          <button
            onClick={onClose}
            data-testid="modal-close"
            className="text-slate-400 hover:text-slate-700 transition-colors text-xl leading-none"
          >
            ×
          </button>
        </div>
        <div className="p-6">{children}</div>
      </div>
    </div>
  );
}

export function Spinner({ label = "Loading..." }) {
  return (
    <div className="flex items-center justify-center gap-3 py-16 text-slate-500">
      <div className="w-5 h-5 border-2 border-slate-300 border-t-[#0D9488] rounded-full animate-spin" />
      <span className="text-sm">{label}</span>
    </div>
  );
}

export function Empty({ children }) {
  return (
    <div className="text-center py-12 text-slate-400 text-sm" data-testid="empty-state">
      {children}
    </div>
  );
}

export function money(v) {
  const n = Number(v || 0);
  return "₹" + n.toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}
