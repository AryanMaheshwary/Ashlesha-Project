import React, { useEffect, useState } from "react";
import { Plus, Pencil, Trash2 } from "lucide-react";
import { toast } from "sonner";
import { api, apiError } from "@/lib/api";
import { useAuth } from "@/context/AuthContext";
import Layout, { PageHeader } from "@/components/Layout";
import { Card, Btn, Field, TextInput, Modal, Spinner, Empty } from "@/components/common";

const blank = { name: "", phone: "", address: "" };

export default function Customers() {
  const { user } = useAuth();
  const canDelete = user.role === "ADMIN" || user.role === "PHARMACIST";
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(blank);
  const [saving, setSaving] = useState(false);

  const load = () => {
    setLoading(true);
    api.customers().then((r) => setItems(r.data)).catch((e) => toast.error(apiError(e))).finally(() => setLoading(false));
  };
  useEffect(load, []);

  const openCreate = () => { setEditing(null); setForm(blank); setOpen(true); };
  const openEdit = (c) => { setEditing(c); setForm({ name: c.name, phone: c.phone || "", address: c.address || "" }); setOpen(true); };

  const save = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      if (editing) { await api.updateCustomer(editing.customerId, form); toast.success("Customer updated"); }
      else { await api.createCustomer(form); toast.success("Customer added"); }
      setOpen(false); load();
    } catch (err) { toast.error(apiError(err)); } finally { setSaving(false); }
  };
  const remove = async (c) => {
    if (!window.confirm(`Delete ${c.name}?`)) return;
    try { await api.deleteCustomer(c.customerId); toast.success("Customer deleted"); load(); }
    catch (err) { toast.error(apiError(err)); }
  };

  return (
    <Layout>
      <div className="px-4 py-6 sm:px-8 sm:py-8">
        <PageHeader title="Customers" subtitle="Manage customer records for billing and prescriptions." actions={<Btn onClick={openCreate} data-testid="add-customer-button"><Plus className="w-4 h-4" /> Add Customer</Btn>} />
        <Card className="overflow-hidden">
          {loading ? <Spinner /> : items.length === 0 ? <Empty>No customers yet.</Empty> : (
            <table className="w-full text-sm">
              <thead className="bg-slate-50 text-slate-500 text-xs uppercase tracking-wider">
                <tr>
                  <th className="text-left px-5 py-3 font-semibold">Name</th>
                  <th className="text-left px-5 py-3 font-semibold">Phone</th>
                  <th className="text-left px-5 py-3 font-semibold">Address</th>
                  <th className="px-5 py-3" />
                </tr>
              </thead>
              <tbody>
                {items.map((c) => (
                  <tr key={c.customerId} className="border-t border-slate-100 hover:bg-slate-50/60" data-testid={`customer-row-${c.customerId}`}>
                    <td className="px-5 py-3 font-medium text-slate-800">{c.name}</td>
                    <td className="px-5 py-3 font-mono-data text-slate-500">{c.phone || "—"}</td>
                    <td className="px-5 py-3 text-slate-500">{c.address || "—"}</td>
                    <td className="px-5 py-3 text-right whitespace-nowrap">
                      <button onClick={() => openEdit(c)} data-testid={`edit-customer-${c.customerId}`} className="text-slate-400 hover:text-[#0D9488] p-1"><Pencil className="w-4 h-4" /></button>
                      {canDelete && <button onClick={() => remove(c)} data-testid={`delete-customer-${c.customerId}`} className="text-slate-400 hover:text-red-600 p-1"><Trash2 className="w-4 h-4" /></button>}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </Card>
      </div>

      <Modal open={open} onClose={() => setOpen(false)} title={editing ? "Edit Customer" : "Add Customer"}>
        <form onSubmit={save} className="space-y-4">
          <Field label="Name"><TextInput data-testid="customer-name-input" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required /></Field>
          <Field label="Phone"><TextInput data-testid="customer-phone-input" value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} /></Field>
          <Field label="Address"><TextInput value={form.address} onChange={(e) => setForm({ ...form, address: e.target.value })} /></Field>
          <div className="flex justify-end gap-2 pt-2">
            <Btn type="button" variant="outline" onClick={() => setOpen(false)}>Cancel</Btn>
            <Btn type="submit" data-testid="customer-save-button" disabled={saving}>{saving ? "Saving…" : "Save"}</Btn>
          </div>
        </form>
      </Modal>
    </Layout>
  );
}
