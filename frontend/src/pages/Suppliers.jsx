import React, { useEffect, useState } from "react";
import { Plus, Pencil, Trash2 } from "lucide-react";
import { toast } from "sonner";
import { api, apiError } from "@/lib/api";
import { useAuth } from "@/context/AuthContext";
import Layout, { PageHeader } from "@/components/Layout";
import { Card, Btn, Field, TextInput, Modal, Spinner, Empty } from "@/components/common";

const blank = { name: "", contactNo: "", email: "", address: "" };

export default function Suppliers() {
  const { user } = useAuth();
  const canDelete = user.role === "ADMIN";
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(blank);
  const [saving, setSaving] = useState(false);

  const load = () => {
    setLoading(true);
    api.suppliers().then((r) => setItems(r.data)).catch((e) => toast.error(apiError(e))).finally(() => setLoading(false));
  };
  useEffect(load, []);

  const openCreate = () => { setEditing(null); setForm(blank); setOpen(true); };
  const openEdit = (s) => { setEditing(s); setForm({ name: s.name, contactNo: s.contactNo || "", email: s.email || "", address: s.address || "" }); setOpen(true); };

  const save = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      if (editing) { await api.updateSupplier(editing.supplierId, form); toast.success("Supplier updated"); }
      else { await api.createSupplier(form); toast.success("Supplier added"); }
      setOpen(false); load();
    } catch (err) { toast.error(apiError(err)); } finally { setSaving(false); }
  };
  const remove = async (s) => {
    if (!window.confirm(`Delete ${s.name}?`)) return;
    try { await api.deleteSupplier(s.supplierId); toast.success("Supplier deleted"); load(); }
    catch (err) { toast.error(apiError(err)); }
  };

  return (
    <Layout>
      <div className="px-4 py-6 sm:px-8 sm:py-8">
        <PageHeader title="Suppliers" subtitle="Manage your medicine suppliers." actions={<Btn onClick={openCreate} data-testid="add-supplier-button"><Plus className="w-4 h-4" /> Add Supplier</Btn>} />
        <Card className="overflow-hidden">
          {loading ? <Spinner /> : items.length === 0 ? <Empty>No suppliers yet.</Empty> : (
            <table className="w-full text-sm">
              <thead className="bg-slate-50 text-slate-500 text-xs uppercase tracking-wider">
                <tr>
                  <th className="text-left px-5 py-3 font-semibold">Name</th>
                  <th className="text-left px-5 py-3 font-semibold">Contact</th>
                  <th className="text-left px-5 py-3 font-semibold">Email</th>
                  <th className="text-left px-5 py-3 font-semibold">Address</th>
                  <th className="px-5 py-3" />
                </tr>
              </thead>
              <tbody>
                {items.map((s) => (
                  <tr key={s.supplierId} className="border-t border-slate-100 hover:bg-slate-50/60" data-testid={`supplier-row-${s.supplierId}`}>
                    <td className="px-5 py-3 font-medium text-slate-800">{s.name}</td>
                    <td className="px-5 py-3 font-mono-data text-slate-500">{s.contactNo || "—"}</td>
                    <td className="px-5 py-3 text-slate-500">{s.email || "—"}</td>
                    <td className="px-5 py-3 text-slate-500">{s.address || "—"}</td>
                    <td className="px-5 py-3 text-right whitespace-nowrap">
                      <button onClick={() => openEdit(s)} data-testid={`edit-supplier-${s.supplierId}`} className="text-slate-400 hover:text-[#0D9488] p-1"><Pencil className="w-4 h-4" /></button>
                      {canDelete && <button onClick={() => remove(s)} data-testid={`delete-supplier-${s.supplierId}`} className="text-slate-400 hover:text-red-600 p-1"><Trash2 className="w-4 h-4" /></button>}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </Card>
      </div>

      <Modal open={open} onClose={() => setOpen(false)} title={editing ? "Edit Supplier" : "Add Supplier"}>
        <form onSubmit={save} className="space-y-4">
          <Field label="Name"><TextInput data-testid="supplier-name-input" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required /></Field>
          <div className="grid grid-cols-2 gap-4">
            <Field label="Contact No"><TextInput data-testid="supplier-contact-input" value={form.contactNo} onChange={(e) => setForm({ ...form, contactNo: e.target.value })} /></Field>
            <Field label="Email"><TextInput type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} /></Field>
          </div>
          <Field label="Address"><TextInput value={form.address} onChange={(e) => setForm({ ...form, address: e.target.value })} /></Field>
          <div className="flex justify-end gap-2 pt-2">
            <Btn type="button" variant="outline" onClick={() => setOpen(false)}>Cancel</Btn>
            <Btn type="submit" data-testid="supplier-save-button" disabled={saving}>{saving ? "Saving…" : "Save"}</Btn>
          </div>
        </form>
      </Modal>
    </Layout>
  );
}
