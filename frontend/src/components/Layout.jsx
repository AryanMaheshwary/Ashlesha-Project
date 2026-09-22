import React from "react";
import { NavLink, useNavigate } from "react-router-dom";
import {
  LayoutDashboard,
  ShoppingCart,
  Pill,
  PackagePlus,
  Truck,
  Users,
  FileText,
  ReceiptText,
  LogOut,
  Cross,
} from "lucide-react";
import { useAuth } from "@/context/AuthContext";
import { RoleBadge, cx } from "@/components/common";

const NAV = [
  { to: "/dashboard", label: "Dashboard", icon: LayoutDashboard, roles: ["ADMIN", "PHARMACIST"] },
  { to: "/billing", label: "Billing", icon: ShoppingCart, roles: ["ADMIN", "PHARMACIST", "CASHIER"] },
  { to: "/inventory", label: "Inventory", icon: Pill, roles: ["ADMIN", "PHARMACIST", "CASHIER"] },
  { to: "/purchases", label: "Stock Entry", icon: PackagePlus, roles: ["ADMIN", "PHARMACIST"] },
  { to: "/suppliers", label: "Suppliers", icon: Truck, roles: ["ADMIN", "PHARMACIST"] },
  { to: "/customers", label: "Customers", icon: Users, roles: ["ADMIN", "PHARMACIST", "CASHIER"] },
  { to: "/prescriptions", label: "Prescriptions", icon: FileText, roles: ["ADMIN", "PHARMACIST", "CASHIER"] },
  { to: "/sales", label: "Sales History", icon: ReceiptText, roles: ["ADMIN", "PHARMACIST", "CASHIER"] },
];

export default function Layout({ children }) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const items = NAV.filter((n) => n.roles.includes(user?.role));

  const doLogout = async () => {
    await logout();
    navigate("/login");
  };

  return (
    <div className="min-h-screen flex bg-[#F8FAFC]">
      <aside className="w-64 bg-[#0F172A] text-slate-100 flex flex-col fixed inset-y-0 left-0">
        <div className="px-6 py-5 border-b border-white/10 flex items-center gap-3">
          <div className="w-9 h-9 rounded-lg bg-[#0D9488] flex items-center justify-center">
            <Cross className="w-5 h-5 text-white" />
          </div>
          <div>
            <div className="font-heading font-bold text-base leading-tight">MediTrack</div>
            <div className="text-[11px] text-slate-400">Pharmacy System</div>
          </div>
        </div>

        <nav className="flex-1 px-3 py-4 space-y-1 overflow-y-auto pms-scroll">
          {items.map((n) => (
            <NavLink
              key={n.to}
              to={n.to}
              data-testid={`nav-${n.label.toLowerCase().replace(/\s+/g, "-")}`}
              className={({ isActive }) =>
                cx(
                  "flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors duration-150",
                  isActive
                    ? "bg-[#1E293B] text-white"
                    : "text-slate-300 hover:bg-white/5 hover:text-white"
                )
              }
            >
              <n.icon className="w-[18px] h-[18px]" />
              {n.label}
            </NavLink>
          ))}
        </nav>

        <div className="px-4 py-4 border-t border-white/10">
          <div className="flex items-center justify-between mb-3">
            <div className="min-w-0">
              <div className="text-sm font-semibold truncate" data-testid="current-user-name">
                {user?.fullName}
              </div>
              <div className="text-xs text-slate-400 truncate">@{user?.username}</div>
            </div>
            <RoleBadge role={user?.role} />
          </div>
          <button
            onClick={doLogout}
            data-testid="logout-button"
            className="w-full flex items-center justify-center gap-2 px-3 py-2 rounded-lg text-sm font-semibold bg-white/5 hover:bg-white/10 text-slate-200 transition-colors"
          >
            <LogOut className="w-4 h-4" /> Sign out
          </button>
        </div>
      </aside>

      <main className="flex-1 ml-64 min-w-0">{children}</main>
    </div>
  );
}

export function PageHeader({ title, subtitle, actions }) {
  return (
    <div className="flex items-start justify-between gap-4 mb-6 flex-wrap">
      <div>
        <h1 className="font-heading text-2xl sm:text-3xl font-bold tracking-tight text-slate-900">
          {title}
        </h1>
        {subtitle && <p className="text-sm text-slate-500 mt-1">{subtitle}</p>}
      </div>
      {actions && <div className="flex items-center gap-2">{actions}</div>}
    </div>
  );
}
