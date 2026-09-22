import React, { useEffect, useState } from "react";
import { Plus, FileText } from "lucide-react";
import { toast } from "sonner";
import { api, apiError } from "@/lib/api";
import Layout, { PageHeader } from "@/components/Layout";
import { Card, Btn, Field, TextInput, Select, Modal, Spinner, Empty } from "@/components/common";

const today = () => new Date().toISOString().slice(0, 10);

export default function Prescriptions() {
  const [items, setItems] = useState([]);
  const [customers, setCustomers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState({ customerId: "", doctorName: "", issueDate: today(), fileReference: "" });
  const [saving, setSaving] = useState(false);

  const load = () => {
    setLoading(true);
    api.prescriptions().then((r) => setItems(r.data)).catch((e) => toast.error(apiError(e))).finally(() => setLoading(false));
  };
  useEffect(() => {
    load();
    api.customers().then((r) => setCustomers(r.data)).catch(() => {});
  }, []);

  const openCreate = () => { setForm({ customerId: "", doctorName: "", issueDate: today(), fileReference: "" }); setOpen(true); };

  const save = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      await api.createPrescription({ ...form, customerId: Number(form.customerId) });
      toast.success("Prescription recorded");
      setOpen(false); load();
    } catch (err) { toast.error(apiError(err)); } finally { setSaving(false); }
  };

  return (
    <Layout>
      <div className="px-4 py-6 sm:px-8 sm:py-8">
        <PageHeader title="Prescriptions" subtitle="Required to sell prescription-only medicines." actions={<Btn onClick={openCreate} data-testid="add-prescription-button"><Plus className="w-4 h-4" /> New Prescription</Btn>} />
        <Card className="overflow-hidden">
          {loading ? <Spinner /> : items.length === 0 ? <Empty>No prescriptions recorded.</Empty> : (
            <table className="w-full text-sm">
              <thead className="bg-slate-50 text-slate-500 text-xs uppercase tracking-wider">
                <tr>
                  <th className="text-left px-5 py-3 font-semibold">#</th>
                  <th className="text-left px-5 py-3 font-semibold">Customer</th>
                  <th className="text-left px-5 py-3 font-semibold">Doctor</th>
                  <th className="text-left px-5 py-3 font-semibold">Issue Date</th>
                  <th className="text-left px-5 py-3 font-semibold">Reference</th>
                </tr>
              </thead>
              <tbody>
                {items.map((p) => (
                  <tr key={p.prescriptionId} className="border-t border-slate-100 hover:bg-slate-50/60" data-testid={`prescription-row-${p.prescriptionId}`}>
                    <td className="px-5 py-3 font-mono-data">#{p.prescriptionId}</td>
                    <td className="px-5 py-3 font-medium text-slate-800">{p.customerName}</td>
                    <td className="px-5 py-3 text-slate-500">Dr. {p.doctorName}</td>
                    <td className="px-5 py-3 font-mono-data text-slate-500">{p.issueDate}</td>
                    <td className="px-5 py-3 text-slate-500 flex items-center gap-1.5"><FileText className="w-3.5 h-3.5 text-slate-400" />{p.fileReference || "—"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </Card>
      </div>

      <Modal open={open} onClose={() => setOpen(false)} title="New Prescription">
        <form onSubmit={save} className="space-y-4">
          <Field label="Customer">
            <Select data-testid="prescription-customer-select" value={form.customerId} onChange={(e) => setForm({ ...form, customerId: e.target.value })} required>
              <option value="">Select customer…</option>
              {customers.map((c) => <option key={c.customerId} value={c.customerId}>{c.name} — {c.phone}</option>)}
            </Select>
          </Field>
          <div className="grid grid-cols-2 gap-4">
            <Field label="Doctor Name"><TextInput data-testid="prescription-doctor-input" value={form.doctorName} onChange={(e) => setForm({ ...form, doctorName: e.target.value })} required /></Field>
            <Field label="Issue Date"><TextInput type="date" value={form.issueDate} onChange={(e) => setForm({ ...form, issueDate: e.target.value })} required /></Field>
          </div>
          <Field label="File Reference" hint="e.g. scanned document name / ID"><TextInput value={form.fileReference} onChange={(e) => setForm({ ...form, fileReference: e.target.value })} /></Field>
          <div className="flex justify-end gap-2 pt-2">
            <Btn type="button" variant="outline" onClick={() => setOpen(false)}>Cancel</Btn>
            <Btn type="submit" data-testid="prescription-save-button" disabled={saving}>{saving ? "Saving…" : "Save"}</Btn>
          </div>
        </form>
      </Modal>
    </Layout>
  );
}
