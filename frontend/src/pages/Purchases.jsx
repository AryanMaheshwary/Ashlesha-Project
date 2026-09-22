import React, { useEffect, useState } from "react";
import { Plus, Trash2, PackagePlus } from "lucide-react";
import { toast } from "sonner";
import { api, apiError } from "@/lib/api";
import Layout, { PageHeader } from "@/components/Layout";
import { Card, Btn, Field, TextInput, Select, Empty, Spinner, money } from "@/components/common";

const today = () => new Date().toISOString().slice(0, 10);
const blankLine = () => ({ medicineId: "", batchNo: "", manufactureDate: "", expiryDate: "", quantity: "", costPrice: "" });

export default function Purchases() {
  const [meds, setMeds] = useState([]);
  const [suppliers, setSuppliers] = useState([]);
  const [purchases, setPurchases] = useState([]);
  const [loading, setLoading] = useState(true);
  const [supplierId, setSupplierId] = useState("");
  const [lines, setLines] = useState([blankLine()]);
  const [saving, setSaving] = useState(false);

  const loadPurchases = () => api.purchases().then((r) => setPurchases(r.data)).catch(() => {});

  useEffect(() => {
    Promise.all([api.medicines(""), api.suppliers(), api.purchases()])
      .then(([m, s, p]) => {
        setMeds(m.data);
        setSuppliers(s.data);
        setPurchases(p.data);
      })
      .catch((e) => toast.error(apiError(e)))
      .finally(() => setLoading(false));
  }, []);

  const updateLine = (i, key, value) => {
    setLines((prev) => prev.map((l, idx) => (idx === i ? { ...l, [key]: value } : l)));
  };
  const addLine = () => setLines((prev) => [...prev, blankLine()]);
  const removeLine = (i) => setLines((prev) => (prev.length === 1 ? prev : prev.filter((_, idx) => idx !== i)));

  const total = lines.reduce((s, l) => s + (parseFloat(l.costPrice) || 0) * (parseInt(l.quantity) || 0), 0);

  const submit = async (e) => {
    e.preventDefault();
    if (!supplierId) return toast.error("Select a supplier");
    setSaving(true);
    try {
      const payload = {
        supplierId: Number(supplierId),
        items: lines.map((l) => ({
          medicineId: Number(l.medicineId),
          batchNo: l.batchNo,
          manufactureDate: l.manufactureDate || null,
          expiryDate: l.expiryDate,
          quantity: Number(l.quantity),
          costPrice: parseFloat(l.costPrice),
        })),
      };
      const res = await api.createPurchase(payload);
      toast.success(`Purchase #${res.data.purchaseId} recorded — new batches added`);
      setLines([blankLine()]);
      setSupplierId("");
      loadPurchases();
      api.medicines("").then((r) => setMeds(r.data)).catch(() => {});
    } catch (err) {
      toast.error(apiError(err));
    } finally {
      setSaving(false);
    }
  };

  return (
    <Layout>
      <div className="px-4 py-6 sm:px-8 sm:py-8">
        <PageHeader title="Stock Entry / Purchases" subtitle="Record incoming stock from suppliers. Each line creates a new batch with its own expiry date." />

        <Card className="p-6 mb-6">
          <form onSubmit={submit}>
            <div className="max-w-sm mb-5">
              <Field label="Supplier">
                <Select data-testid="purchase-supplier-select" value={supplierId} onChange={(e) => setSupplierId(e.target.value)} required>
                  <option value="">Select supplier…</option>
                  {suppliers.map((s) => <option key={s.supplierId} value={s.supplierId}>{s.name}</option>)}
                </Select>
              </Field>
            </div>

            <div className="overflow-x-auto pms-scroll">
              <table className="w-full text-sm min-w-[820px]">
                <thead className="text-xs uppercase tracking-wider text-slate-500">
                  <tr className="text-left">
                    <th className="py-2 pr-2 font-semibold">Medicine</th>
                    <th className="py-2 px-2 font-semibold">Batch No</th>
                    <th className="py-2 px-2 font-semibold">Mfg Date</th>
                    <th className="py-2 px-2 font-semibold">Expiry Date</th>
                    <th className="py-2 px-2 font-semibold">Qty</th>
                    <th className="py-2 px-2 font-semibold">Cost ₹</th>
                    <th />
                  </tr>
                </thead>
                <tbody>
                  {lines.map((l, i) => (
                    <tr key={i} data-testid={`purchase-line-${i}`}>
                      <td className="py-1.5 pr-2">
                        <Select data-testid={`purchase-medicine-${i}`} value={l.medicineId} onChange={(e) => updateLine(i, "medicineId", e.target.value)} required>
                          <option value="">Select…</option>
                          {meds.map((m) => <option key={m.medicineId} value={m.medicineId}>{m.name}</option>)}
                        </Select>
                      </td>
                      <td className="py-1.5 px-2"><TextInput data-testid={`purchase-batchno-${i}`} value={l.batchNo} onChange={(e) => updateLine(i, "batchNo", e.target.value)} placeholder="B-001" required /></td>
                      <td className="py-1.5 px-2"><TextInput type="date" value={l.manufactureDate} onChange={(e) => updateLine(i, "manufactureDate", e.target.value)} /></td>
                      <td className="py-1.5 px-2"><TextInput data-testid={`purchase-expiry-${i}`} type="date" min={today()} value={l.expiryDate} onChange={(e) => updateLine(i, "expiryDate", e.target.value)} required /></td>
                      <td className="py-1.5 px-2 w-24"><TextInput data-testid={`purchase-qty-${i}`} type="number" min="1" value={l.quantity} onChange={(e) => updateLine(i, "quantity", e.target.value)} required /></td>
                      <td className="py-1.5 px-2 w-28"><TextInput data-testid={`purchase-cost-${i}`} type="number" step="0.01" min="0" value={l.costPrice} onChange={(e) => updateLine(i, "costPrice", e.target.value)} required /></td>
                      <td className="py-1.5 px-2">
                        <button type="button" onClick={() => removeLine(i)} className="text-slate-300 hover:text-red-500"><Trash2 className="w-4 h-4" /></button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div className="flex items-center justify-between mt-4">
              <Btn type="button" variant="outline" onClick={addLine} data-testid="add-purchase-line"><Plus className="w-4 h-4" /> Add line</Btn>
              <div className="flex items-center gap-6">
                <div className="text-sm text-slate-500">Total: <span className="font-heading text-lg font-bold font-mono-data text-slate-900">{money(total)}</span></div>
                <Btn type="submit" data-testid="record-purchase-button" disabled={saving}><PackagePlus className="w-4 h-4" /> {saving ? "Recording…" : "Record Purchase"}</Btn>
              </div>
            </div>
          </form>
        </Card>

        <Card className="overflow-hidden">
          <div className="px-5 py-3 border-b border-slate-100 font-heading font-semibold">Recent Purchases</div>
          {loading ? <Spinner /> : purchases.length === 0 ? <Empty>No purchases recorded yet.</Empty> : (
            <table className="w-full text-sm">
              <thead className="bg-slate-50 text-slate-500 text-xs uppercase tracking-wider">
                <tr>
                  <th className="text-left px-5 py-3 font-semibold">#</th>
                  <th className="text-left px-5 py-3 font-semibold">Supplier</th>
                  <th className="text-left px-5 py-3 font-semibold">Recorded By</th>
                  <th className="text-left px-5 py-3 font-semibold">Date</th>
                  <th className="text-right px-5 py-3 font-semibold">Amount</th>
                </tr>
              </thead>
              <tbody>
                {purchases.map((p) => (
                  <tr key={p.purchaseId} className="border-t border-slate-100" data-testid={`purchase-row-${p.purchaseId}`}>
                    <td className="px-5 py-3 font-mono-data">#{p.purchaseId}</td>
                    <td className="px-5 py-3 font-medium">{p.supplierName}</td>
                    <td className="px-5 py-3 text-slate-500">{p.userName}</td>
                    <td className="px-5 py-3 font-mono-data text-slate-500">{String(p.purchaseDate).replace("T", " ").slice(0, 16)}</td>
                    <td className="px-5 py-3 text-right font-mono-data font-semibold">{money(p.totalAmount)}</td>
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
