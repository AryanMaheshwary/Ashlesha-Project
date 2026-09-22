import React, { useEffect, useState } from "react";
import { Search, Plus, Minus, Trash2, ShoppingCart, Printer, CheckCircle2, FileText } from "lucide-react";
import { toast } from "sonner";
import { api, apiError } from "@/lib/api";
import Layout, { PageHeader } from "@/components/Layout";
import { Card, Btn, Select, TextInput, Modal, Empty, money, cx } from "@/components/common";

export default function Billing() {
  const [search, setSearch] = useState("");
  const [meds, setMeds] = useState([]);
  const [cart, setCart] = useState([]);
  const [customers, setCustomers] = useState([]);
  const [prescriptions, setPrescriptions] = useState([]);
  const [customerId, setCustomerId] = useState("");
  const [prescriptionId, setPrescriptionId] = useState("");
  const [paymentMode, setPaymentMode] = useState("CASH");
  const [submitting, setSubmitting] = useState(false);
  const [invoice, setInvoice] = useState(null);

  useEffect(() => {
    const t = setTimeout(() => {
      api.medicines(search).then((r) => setMeds(r.data)).catch(() => {});
    }, 200);
    return () => clearTimeout(t);
  }, [search]);

  useEffect(() => {
    api.customers().then((r) => setCustomers(r.data)).catch(() => {});
    api.prescriptions().then((r) => setPrescriptions(r.data)).catch(() => {});
  }, []);

  const addToCart = (m) => {
    setCart((prev) => {
      const found = prev.find((c) => c.medicineId === m.medicineId);
      const inCart = found ? found.quantity : 0;
      if (inCart + 1 > m.totalAvailable) {
        toast.error(`Only ${m.totalAvailable} ${m.unit} of ${m.name} in stock`);
        return prev;
      }
      if (found) {
        return prev.map((c) => (c.medicineId === m.medicineId ? { ...c, quantity: c.quantity + 1 } : c));
      }
      return [...prev, {
        medicineId: m.medicineId,
        name: m.name,
        unitPrice: Number(m.unitPrice),
        unit: m.unit,
        prescriptionRequired: m.prescriptionRequired,
        totalAvailable: m.totalAvailable,
        quantity: 1,
      }];
    });
  };

  const setQty = (id, delta) => {
    setCart((prev) =>
      prev
        .map((c) => {
          if (c.medicineId !== id) return c;
          const q = c.quantity + delta;
          if (q > c.totalAvailable) {
            toast.error(`Only ${c.totalAvailable} ${c.unit} available`);
            return c;
          }
          return { ...c, quantity: q };
        })
        .filter((c) => c.quantity > 0)
    );
  };

  const removeItem = (id) => setCart((prev) => prev.filter((c) => c.medicineId !== id));

  const total = cart.reduce((s, c) => s + c.unitPrice * c.quantity, 0);
  const needsRx = cart.some((c) => c.prescriptionRequired);

  const checkout = async () => {
    if (cart.length === 0) return toast.error("Cart is empty");
    setSubmitting(true);
    try {
      const payload = {
        customerId: customerId ? Number(customerId) : null,
        prescriptionId: prescriptionId ? Number(prescriptionId) : null,
        paymentMode,
        items: cart.map((c) => ({ medicineId: c.medicineId, quantity: c.quantity })),
      };
      const res = await api.createSale(payload);
      setInvoice(res.data);
      setCart([]);
      setCustomerId("");
      setPrescriptionId("");
      toast.success(`Invoice #${res.data.saleId} generated`);
      api.medicines(search).then((r) => setMeds(r.data)).catch(() => {});
    } catch (err) {
      toast.error(apiError(err));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Layout>
      <div className="px-4 py-6 sm:px-8 sm:py-8">
        <PageHeader title="Billing / Point of Sale" subtitle="Stock is deducted FIFO from the batch nearest to expiry." />

        <div className="grid grid-cols-1 lg:grid-cols-5 gap-6">
          {/* Catalog */}
          <div className="lg:col-span-3">
            <div className="relative mb-4">
              <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
              <TextInput
                data-testid="billing-search-input"
                className="pl-9"
                placeholder="Search medicine to add…"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </div>
            <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
              {meds.map((m) => {
                const out = m.totalAvailable <= 0;
                return (
                  <button
                    key={m.medicineId}
                    disabled={out}
                    onClick={() => addToCart(m)}
                    data-testid={`billing-med-${m.medicineId}`}
                    className={cx(
                      "text-left bg-white border border-slate-200 rounded-lg p-4 transition-colors",
                      out ? "opacity-50 cursor-not-allowed" : "hover:border-[#0D9488] hover:shadow-sm"
                    )}
                  >
                    <div className="flex items-start justify-between gap-2">
                      <span className="font-semibold text-sm text-slate-800 leading-tight">{m.name}</span>
                      {m.prescriptionRequired && (
                        <span className="shrink-0 px-1.5 py-0.5 rounded text-[10px] font-bold bg-red-100 text-red-700">Rx</span>
                      )}
                    </div>
                    <div className="text-xs text-slate-400 mt-1">{m.category}</div>
                    <div className="flex items-center justify-between mt-3">
                      <span className="font-mono-data font-bold text-[#0F766E]">{money(m.unitPrice)}</span>
                      <span className={cx("text-xs font-mono-data", out ? "text-red-500" : "text-slate-400")}>
                        {out ? "Out of stock" : `${m.totalAvailable} ${m.unit}`}
                      </span>
                    </div>
                  </button>
                );
              })}
              {meds.length === 0 && <div className="col-span-full"><Empty>No medicines match your search.</Empty></div>}
            </div>
          </div>

          {/* Cart */}
          <div className="lg:col-span-2">
            <Card className="p-5 sticky top-6">
              <div className="flex items-center gap-2 mb-4">
                <ShoppingCart className="w-5 h-5 text-[#0D9488]" />
                <h3 className="font-heading text-lg font-semibold">Cart</h3>
                <span className="ml-auto text-xs text-slate-400">{cart.length} item(s)</span>
              </div>

              <div className="space-y-2 max-h-[280px] overflow-y-auto pms-scroll mb-4">
                {cart.length === 0 ? (
                  <Empty>Click a medicine to add it.</Empty>
                ) : (
                  cart.map((c) => (
                    <div key={c.medicineId} className="flex items-center gap-2 bg-slate-50 rounded-lg p-2" data-testid={`cart-item-${c.medicineId}`}>
                      <div className="min-w-0 flex-1">
                        <div className="text-sm font-medium text-slate-800 truncate">{c.name}</div>
                        <div className="text-xs text-slate-400 font-mono-data">{money(c.unitPrice)} × {c.quantity}</div>
                      </div>
                      <div className="flex items-center gap-1">
                        <button onClick={() => setQty(c.medicineId, -1)} data-testid={`cart-dec-${c.medicineId}`} className="w-6 h-6 rounded bg-white border border-slate-200 flex items-center justify-center hover:bg-slate-100">
                          <Minus className="w-3 h-3" />
                        </button>
                        <span className="w-7 text-center text-sm font-mono-data font-semibold" data-testid={`cart-qty-${c.medicineId}`}>{c.quantity}</span>
                        <button onClick={() => setQty(c.medicineId, 1)} data-testid={`cart-inc-${c.medicineId}`} className="w-6 h-6 rounded bg-white border border-slate-200 flex items-center justify-center hover:bg-slate-100">
                          <Plus className="w-3 h-3" />
                        </button>
                      </div>
                      <div className="w-20 text-right font-mono-data text-sm font-semibold">{money(c.unitPrice * c.quantity)}</div>
                      <button onClick={() => removeItem(c.medicineId)} className="text-slate-300 hover:text-red-500">
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  ))
                )}
              </div>

              {needsRx && (
                <div className="flex items-start gap-2 text-xs bg-red-50 text-red-700 border border-red-200 rounded-lg p-2.5 mb-3">
                  <FileText className="w-4 h-4 shrink-0 mt-0.5" />
                  <span>Cart contains prescription-only medicine. Link a prescription below.</span>
                </div>
              )}

              <div className="space-y-3 border-t border-slate-100 pt-4">
                <Select data-testid="billing-customer-select" value={customerId} onChange={(e) => setCustomerId(e.target.value)}>
                  <option value="">Walk-in customer (optional)</option>
                  {customers.map((c) => <option key={c.customerId} value={c.customerId}>{c.name} — {c.phone}</option>)}
                </Select>
                <Select data-testid="billing-prescription-select" value={prescriptionId} onChange={(e) => setPrescriptionId(e.target.value)}>
                  <option value="">No prescription linked</option>
                  {prescriptions.map((p) => <option key={p.prescriptionId} value={p.prescriptionId}>#{p.prescriptionId} · {p.customerName} · Dr. {p.doctorName}</option>)}
                </Select>
                <div className="flex gap-2">
                  {["CASH", "CARD", "UPI"].map((m) => (
                    <button
                      key={m}
                      onClick={() => setPaymentMode(m)}
                      data-testid={`payment-${m.toLowerCase()}`}
                      className={cx(
                        "flex-1 py-2 rounded-lg text-xs font-semibold border transition-colors",
                        paymentMode === m ? "bg-[#0F172A] text-white border-[#0F172A]" : "bg-white text-slate-600 border-slate-200 hover:border-slate-300"
                      )}
                    >
                      {m}
                    </button>
                  ))}
                </div>
              </div>

              <div className="flex items-center justify-between mt-4 mb-3">
                <span className="text-sm text-slate-500">Total</span>
                <span className="font-heading text-2xl font-bold font-mono-data" data-testid="cart-total">{money(total)}</span>
              </div>
              <Btn onClick={checkout} data-testid="checkout-button" className="w-full" disabled={submitting || cart.length === 0}>
                {submitting ? "Processing…" : "Generate Invoice"}
              </Btn>
            </Card>
          </div>
        </div>
      </div>

      <Modal open={!!invoice} onClose={() => setInvoice(null)} title="Invoice" wide>
        {invoice && <Invoice invoice={invoice} />}
      </Modal>
    </Layout>
  );
}

function Invoice({ invoice }) {
  return (
    <div>
      <div id="invoice-print" className="border border-slate-200 rounded-lg p-6">
        <div className="flex items-center justify-between mb-6">
          <div>
            <div className="font-heading text-xl font-bold text-slate-900">MediTrack Pharmacy</div>
            <div className="text-xs text-slate-400">Tax Invoice</div>
          </div>
          <div className="text-right">
            <div className="flex items-center gap-2 text-emerald-600 font-semibold text-sm justify-end">
              <CheckCircle2 className="w-4 h-4" /> Paid · {invoice.paymentMode}
            </div>
            <div className="font-mono-data text-sm mt-1">Invoice #{invoice.saleId}</div>
          </div>
        </div>
        <div className="grid grid-cols-2 gap-4 text-sm mb-4">
          <div>
            <div className="text-xs text-slate-400 uppercase tracking-wider">Customer</div>
            <div className="font-medium">{invoice.customerName || "Walk-in customer"}</div>
          </div>
          <div className="text-right">
            <div className="text-xs text-slate-400 uppercase tracking-wider">Date</div>
            <div className="font-medium font-mono-data">{String(invoice.saleDate).replace("T", " ").slice(0, 16)}</div>
          </div>
        </div>
        <table className="w-full text-sm border-t border-slate-200">
          <thead className="text-xs uppercase tracking-wider text-slate-400">
            <tr>
              <th className="text-left py-2">Medicine</th>
              <th className="text-left py-2">Batch</th>
              <th className="text-right py-2">Qty</th>
              <th className="text-right py-2">Price</th>
              <th className="text-right py-2">Amount</th>
            </tr>
          </thead>
          <tbody>
            {invoice.items.map((it) => (
              <tr key={it.saleItemId} className="border-t border-slate-100">
                <td className="py-2 font-medium">{it.medicineName}</td>
                <td className="py-2 font-mono-data text-slate-500 text-xs">
                  <span className="inline-flex items-center px-2 py-0.5 rounded-full bg-green-100 text-green-700">{it.batchNo}</span>
                </td>
                <td className="py-2 text-right font-mono-data">{it.quantity}</td>
                <td className="py-2 text-right font-mono-data">{money(it.sellingPrice)}</td>
                <td className="py-2 text-right font-mono-data">{money(it.sellingPrice * it.quantity)}</td>
              </tr>
            ))}
          </tbody>
          <tfoot>
            <tr className="border-t-2 border-slate-200">
              <td colSpan={4} className="py-3 text-right font-semibold">Total</td>
              <td className="py-3 text-right font-heading text-lg font-bold font-mono-data">{money(invoice.totalAmount)}</td>
            </tr>
          </tfoot>
        </table>
        <div className="text-xs text-slate-400 mt-4 text-center">Batches auto-selected FIFO by nearest expiry. Thank you!</div>
      </div>
      <div className="flex justify-end mt-4">
        <Btn onClick={() => window.print()} data-testid="print-invoice-button">
          <Printer className="w-4 h-4" /> Print
        </Btn>
      </div>
    </div>
  );
}
