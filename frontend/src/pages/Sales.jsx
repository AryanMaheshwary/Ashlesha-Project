import React, { useEffect, useState } from "react";
import { Eye } from "lucide-react";
import { toast } from "sonner";
import { api, apiError } from "@/lib/api";
import Layout, { PageHeader } from "@/components/Layout";
import { Card, Modal, Spinner, Empty, money, cx } from "@/components/common";

export default function Sales() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [detail, setDetail] = useState(null);

  useEffect(() => {
    api.sales().then((r) => setItems(r.data)).catch((e) => toast.error(apiError(e))).finally(() => setLoading(false));
  }, []);

  const view = async (id) => {
    try { const r = await api.sale(id); setDetail(r.data); }
    catch (err) { toast.error(apiError(err)); }
  };

  const badge = (mode) => ({
    CASH: "bg-emerald-100 text-emerald-700",
    CARD: "bg-blue-100 text-blue-700",
    UPI: "bg-purple-100 text-purple-700",
  }[mode] || "bg-slate-100 text-slate-600");

  return (
    <Layout>
      <div className="px-4 py-6 sm:px-8 sm:py-8">
        <PageHeader title="Sales History" subtitle="All invoices generated at the counter." />
        <Card className="overflow-hidden">
          {loading ? <Spinner /> : items.length === 0 ? <Empty>No sales yet.</Empty> : (
            <table className="w-full text-sm">
              <thead className="bg-slate-50 text-slate-500 text-xs uppercase tracking-wider">
                <tr>
                  <th className="text-left px-5 py-3 font-semibold">Invoice</th>
                  <th className="text-left px-5 py-3 font-semibold">Customer</th>
                  <th className="text-left px-5 py-3 font-semibold">Cashier</th>
                  <th className="text-left px-5 py-3 font-semibold">Date</th>
                  <th className="text-center px-5 py-3 font-semibold">Payment</th>
                  <th className="text-right px-5 py-3 font-semibold">Amount</th>
                  <th className="px-5 py-3" />
                </tr>
              </thead>
              <tbody>
                {items.map((s) => (
                  <tr key={s.saleId} className="border-t border-slate-100 hover:bg-slate-50/60" data-testid={`sale-row-${s.saleId}`}>
                    <td className="px-5 py-3 font-mono-data font-semibold">#{s.saleId}</td>
                    <td className="px-5 py-3 text-slate-700">{s.customerName || "Walk-in"}</td>
                    <td className="px-5 py-3 text-slate-500">{s.userName}</td>
                    <td className="px-5 py-3 font-mono-data text-slate-500">{String(s.saleDate).replace("T", " ").slice(0, 16)}</td>
                    <td className="px-5 py-3 text-center"><span className={cx("inline-flex px-2.5 py-0.5 rounded-full text-xs font-semibold", badge(s.paymentMode))}>{s.paymentMode}</span></td>
                    <td className="px-5 py-3 text-right font-mono-data font-semibold">{money(s.totalAmount)}</td>
                    <td className="px-5 py-3 text-right">
                      <button onClick={() => view(s.saleId)} data-testid={`view-sale-${s.saleId}`} className="text-slate-400 hover:text-[#0D9488] p-1"><Eye className="w-4 h-4" /></button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </Card>
      </div>

      <Modal open={!!detail} onClose={() => setDetail(null)} title={detail ? `Invoice #${detail.saleId}` : ""} wide>
        {detail && (
          <div>
            <div className="grid grid-cols-2 gap-4 text-sm mb-4">
              <div><div className="text-xs text-slate-400 uppercase tracking-wider">Customer</div><div className="font-medium">{detail.customerName || "Walk-in"}</div></div>
              <div className="text-right"><div className="text-xs text-slate-400 uppercase tracking-wider">Payment</div><div className="font-medium">{detail.paymentMode}</div></div>
            </div>
            <table className="w-full text-sm border-t border-slate-200">
              <thead className="text-xs uppercase tracking-wider text-slate-400">
                <tr><th className="text-left py-2">Medicine</th><th className="text-left py-2">Batch</th><th className="text-right py-2">Qty</th><th className="text-right py-2">Price</th><th className="text-right py-2">Amount</th></tr>
              </thead>
              <tbody>
                {(detail.items || []).map((it) => (
                  <tr key={it.saleItemId} className="border-t border-slate-100">
                    <td className="py-2 font-medium">{it.medicineName}</td>
                    <td className="py-2 font-mono-data text-xs text-slate-500">{it.batchNo}</td>
                    <td className="py-2 text-right font-mono-data">{it.quantity}</td>
                    <td className="py-2 text-right font-mono-data">{money(it.sellingPrice)}</td>
                    <td className="py-2 text-right font-mono-data">{money(it.sellingPrice * it.quantity)}</td>
                  </tr>
                ))}
              </tbody>
              <tfoot><tr className="border-t-2 border-slate-200"><td colSpan={4} className="py-3 text-right font-semibold">Total</td><td className="py-3 text-right font-heading text-lg font-bold font-mono-data">{money(detail.totalAmount)}</td></tr></tfoot>
            </table>
          </div>
        )}
      </Modal>
    </Layout>
  );
}
