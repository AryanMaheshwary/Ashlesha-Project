import React, { useEffect, useState } from "react";
import {
  ResponsiveContainer,
  AreaChart,
  Area,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
} from "recharts";
import { IndianRupee, TrendingUp, PackageX, CalendarClock, AlertTriangle } from "lucide-react";
import { toast } from "sonner";
import { api, apiError } from "@/lib/api";
import Layout, { PageHeader } from "@/components/Layout";
import { Card, Spinner, Empty, money, cx } from "@/components/common";

function Metric({ icon: Icon, label, value, tone }) {
  const tones = {
    teal: "bg-teal-50 text-teal-700",
    emerald: "bg-emerald-50 text-emerald-700",
    amber: "bg-amber-50 text-amber-700",
    red: "bg-red-50 text-red-700",
  };
  return (
    <Card className="p-5">
      <div className="flex items-center justify-between">
        <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">{label}</span>
        <div className={cx("w-9 h-9 rounded-lg flex items-center justify-center", tones[tone])}>
          <Icon className="w-[18px] h-[18px]" />
        </div>
      </div>
      <div className="mt-3 font-heading text-2xl font-bold text-slate-900 font-mono-data" data-testid={`metric-${label.toLowerCase().replace(/\s+/g, "-")}`}>
        {value}
      </div>
    </Card>
  );
}

export default function Dashboard() {
  const [summary, setSummary] = useState(null);
  const [revenue, setRevenue] = useState([]);
  const [period, setPeriod] = useState("daily");
  const [top, setTop] = useState([]);
  const [lowStock, setLowStock] = useState([]);
  const [expiry, setExpiry] = useState([]);
  const [tab, setTab] = useState("low");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([
      api.reportSummary(),
      api.reportTopSelling(6),
      api.reportLowStock(20),
      api.reportExpiry(30),
    ])
      .then(([s, t, l, e]) => {
        setSummary(s.data);
        setTop(t.data);
        setLowStock(l.data);
        setExpiry(e.data);
      })
      .catch((err) => toast.error(apiError(err)))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    api.reportRevenue(period).then((r) => setRevenue(r.data)).catch(() => {});
  }, [period]);

  if (loading) {
    return (
      <Layout>
        <div className="p-8"><Spinner /></div>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="px-4 py-6 sm:px-8 sm:py-8">
        <PageHeader title="Reports Dashboard" subtitle="Live overview of revenue, stock and alerts." />

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 sm:gap-6 mb-6">
          <Metric icon={IndianRupee} label="Daily Revenue" value={money(summary?.dailyRevenue)} tone="teal" />
          <Metric icon={TrendingUp} label="Monthly Sales" value={money(summary?.monthlyRevenue)} tone="emerald" />
          <Metric icon={PackageX} label="Low Stock Items" value={summary?.lowStockCount ?? 0} tone="amber" />
          <Metric icon={CalendarClock} label="Expiring 30d" value={summary?.expiringCount ?? 0} tone="red" />
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-6">
          <Card className="p-6 lg:col-span-2">
            <div className="flex items-center justify-between mb-4">
              <h3 className="font-heading text-lg font-semibold">Revenue Trend</h3>
              <div className="flex rounded-lg border border-slate-200 overflow-hidden">
                {["daily", "monthly"].map((p) => (
                  <button
                    key={p}
                    data-testid={`revenue-period-${p}`}
                    onClick={() => setPeriod(p)}
                    className={cx(
                      "px-3 py-1.5 text-xs font-semibold capitalize transition-colors",
                      period === p ? "bg-[#0D9488] text-white" : "bg-white text-slate-600 hover:bg-slate-50"
                    )}
                  >
                    {p}
                  </button>
                ))}
              </div>
            </div>
            {revenue.length === 0 ? (
              <Empty>No sales recorded yet.</Empty>
            ) : (
              <ResponsiveContainer width="100%" height={260}>
                <AreaChart data={revenue} margin={{ left: -10, right: 10, top: 10 }}>
                  <defs>
                    <linearGradient id="rev" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="0%" stopColor="#0D9488" stopOpacity={0.35} />
                      <stop offset="100%" stopColor="#0D9488" stopOpacity={0} />
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" vertical={false} />
                  <XAxis dataKey="period" tick={{ fontSize: 11, fill: "#64748b" }} />
                  <YAxis tick={{ fontSize: 11, fill: "#64748b" }} />
                  <Tooltip formatter={(v) => money(v)} />
                  <Area type="monotone" dataKey="revenue" stroke="#0D9488" strokeWidth={2} fill="url(#rev)" />
                </AreaChart>
              </ResponsiveContainer>
            )}
          </Card>

          <Card className="p-6">
            <h3 className="font-heading text-lg font-semibold mb-4">Top-Selling Medicines</h3>
            {top.length === 0 ? (
              <Empty>No sales yet.</Empty>
            ) : (
              <ResponsiveContainer width="100%" height={260}>
                <BarChart data={top} layout="vertical" margin={{ left: 10, right: 10 }}>
                  <XAxis type="number" hide />
                  <YAxis
                    type="category"
                    dataKey="name"
                    width={90}
                    tick={{ fontSize: 10, fill: "#475569" }}
                    tickFormatter={(v) => (v.length > 14 ? v.slice(0, 13) + "…" : v)}
                  />
                  <Tooltip formatter={(v, n) => (n === "quantitySold" ? [v, "Units"] : v)} />
                  <Bar dataKey="quantitySold" fill="#059669" radius={[0, 4, 4, 0]} />
                </BarChart>
              </ResponsiveContainer>
            )}
          </Card>
        </div>

        <Card className="p-0 overflow-hidden">
          <div className="flex border-b border-slate-200">
            <button
              data-testid="alerts-tab-low"
              onClick={() => setTab("low")}
              className={cx(
                "px-5 py-3 text-sm font-semibold transition-colors flex items-center gap-2",
                tab === "low" ? "text-amber-700 border-b-2 border-amber-500" : "text-slate-500 hover:text-slate-700"
              )}
            >
              <AlertTriangle className="w-4 h-4" /> Low Stock ({lowStock.length})
            </button>
            <button
              data-testid="alerts-tab-expiry"
              onClick={() => setTab("expiry")}
              className={cx(
                "px-5 py-3 text-sm font-semibold transition-colors flex items-center gap-2",
                tab === "expiry" ? "text-red-700 border-b-2 border-red-500" : "text-slate-500 hover:text-slate-700"
              )}
            >
              <CalendarClock className="w-4 h-4" /> Expiring ≤ 30d ({expiry.length})
            </button>
          </div>

          {tab === "low" ? (
            lowStock.length === 0 ? (
              <Empty>All medicines are sufficiently stocked.</Empty>
            ) : (
              <table className="w-full text-sm">
                <thead className="bg-slate-50 text-slate-500 text-xs uppercase tracking-wider">
                  <tr>
                    <th className="text-left px-5 py-3 font-semibold">Medicine</th>
                    <th className="text-left px-5 py-3 font-semibold">Category</th>
                    <th className="text-right px-5 py-3 font-semibold">Available</th>
                  </tr>
                </thead>
                <tbody>
                  {lowStock.map((m) => (
                    <tr key={m.medicineId} className="border-t border-slate-100" data-testid={`low-stock-row-${m.medicineId}`}>
                      <td className="px-5 py-3 font-medium text-slate-800">{m.name}</td>
                      <td className="px-5 py-3 text-slate-500">{m.category}</td>
                      <td className="px-5 py-3 text-right">
                        <span className="inline-flex px-2.5 py-0.5 rounded-full text-xs font-semibold bg-amber-100 text-amber-800 font-mono-data">
                          {m.totalAvailable} {m.unit}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )
          ) : expiry.length === 0 ? (
            <Empty>No batches expiring within 30 days.</Empty>
          ) : (
            <table className="w-full text-sm">
              <thead className="bg-slate-50 text-slate-500 text-xs uppercase tracking-wider">
                <tr>
                  <th className="text-left px-5 py-3 font-semibold">Medicine</th>
                  <th className="text-left px-5 py-3 font-semibold">Batch</th>
                  <th className="text-left px-5 py-3 font-semibold">Expiry</th>
                  <th className="text-right px-5 py-3 font-semibold">Qty</th>
                </tr>
              </thead>
              <tbody>
                {expiry.map((b) => (
                  <tr key={b.batchId} className="border-t border-slate-100" data-testid={`expiry-row-${b.batchId}`}>
                    <td className="px-5 py-3 font-medium text-slate-800">{b.medicineName}</td>
                    <td className="px-5 py-3 font-mono-data text-slate-500">{b.batchNo}</td>
                    <td className="px-5 py-3">
                      <span className="inline-flex px-2.5 py-0.5 rounded-full text-xs font-semibold bg-red-100 text-red-800 font-mono-data">
                        {b.expiryDate}
                      </span>
                    </td>
                    <td className="px-5 py-3 text-right font-mono-data">{b.quantityAvailable}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </Card>
      </div>
    </Layout>
  );
}
