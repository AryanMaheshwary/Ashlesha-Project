import React, { useEffect, useState, useCallback } from "react";
import { Plus, Search, Pencil, Trash2, ChevronDown, ChevronRight, Layers } from "lucide-react";
import { toast } from "sonner";
import { api, apiError } from "@/lib/api";
import { useAuth } from "@/context/AuthContext";
import Layout, { PageHeader } from "@/components/Layout";
import { Card, Btn, Field, TextInput, Select, Modal, Spinner, Empty, money, cx } from "@/components/common";

const emptyForm = {
  name: "",
  category: "",
  manufacturer: "",
  unit: "tablet",
  unitPrice: "",
  prescriptionRequired: false,
};

export default function Inventory() {
  const { user } = useAuth();
  const canEdit = user.role === "ADMIN" || user.role === "PHARMACIST";
  const canDelete = user.role === "ADMIN";

  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [saving, setSaving] = useState(false);
  const [expanded, setExpanded] = useState(null);
  const [batches, setBatches] = useState([]);

  const load = useCallback((term) => {
    setLoading(true);
    api
      .medicines(term)
      .then((r) => setItems(r.data))
      .catch((e) => toast.error(apiError(e)))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    const t = setTimeout(() => load(search), 250);
    return () => clearTimeout(t);
  }, [search, load]);

  const openCreate = () => {
    setEditing(null);
    setForm(emptyForm);
    setOpen(true);
  };
  const openEdit = (m) => {
    setEditing(m);
    setForm({
      name: m.name,
      category: m.category,
      manufacturer: m.manufacturer || "",
      unit: m.unit,
      unitPrice: String(m.unitPrice),
      prescriptionRequired: m.prescriptionRequired,
    });
    setOpen(true);
  };

  const save = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      const payload = { ...form, unitPrice: parseFloat(form.unitPrice) };
      if (editing) {
        await api.updateMedicine(editing.medicineId, payload);
        toast.success("Medicine updated");
      } else {
        await api.createMedicine(payload);
        toast.success("Medicine added");
      }
      setOpen(false);
      load(search);
    } catch (err) {
      toast.error(apiError(err));
    } finally {
      setSaving(false);
    }
  };

  const remove = async (m) => {
    if (!window.confirm(`Delete ${m.name}?`)) return;
    try {
      await api.deleteMedicine(m.medicineId);
      toast.success("Medicine deleted");
      load(search);
    } catch (err) {
      toast.error(apiError(err));
    }
  };

  const toggleBatches = async (m) => {
    if (expanded === m.medicineId) {
      setExpanded(null);
      return;
    }
    setExpanded(m.medicineId);
    setBatches([]);
    try {
      const r = await api.batches(m.medicineId);
      setBatches(r.data);
    } catch (err) {
      toast.error(apiError(err));
    }
  };

  return (
    <Layout>
      <div className="px-4 py-6 sm:px-8 sm:py-8">
        <PageHeader
          title="Medicine Inventory"
          subtitle="Catalog with batch-level stock tracking."
          actions={
            canEdit && (
              <Btn onClick={openCreate} data-testid="add-medicine-button">
                <Plus className="w-4 h-4" /> Add Medicine
              </Btn>
            )
          }
        />

        <div className="relative mb-4 max-w-md">
          <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
          <TextInput
            data-testid="medicine-search-input"
            className="pl-9"
            placeholder="Search by name, category or manufacturer…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>

        <Card className="overflow-hidden">
          {loading ? (
            <Spinner />
          ) : items.length === 0 ? (
            <Empty>No medicines found.</Empty>
          ) : (
            <table className="w-full text-sm">
              <thead className="bg-slate-50 text-slate-500 text-xs uppercase tracking-wider sticky top-0">
                <tr>
                  <th className="w-8" />
                  <th className="text-left px-4 py-3 font-semibold">Name</th>
                  <th className="text-left px-4 py-3 font-semibold">Category</th>
                  <th className="text-left px-4 py-3 font-semibold">Manufacturer</th>
                  <th className="text-right px-4 py-3 font-semibold">Price</th>
                  <th className="text-right px-4 py-3 font-semibold">Stock</th>
                  <th className="text-center px-4 py-3 font-semibold">Rx</th>
                  <th className="px-4 py-3" />
                </tr>
              </thead>
              <tbody>
                {items.map((m) => (
                  <React.Fragment key={m.medicineId}>
                    <tr className="border-t border-slate-100 hover:bg-slate-50/60 transition-colors" data-testid={`medicine-row-${m.medicineId}`}>
                      <td className="pl-4">
                        <button onClick={() => toggleBatches(m)} data-testid={`batch-toggle-${m.medicineId}`} className="text-slate-400 hover:text-slate-700">
                          {expanded === m.medicineId ? <ChevronDown className="w-4 h-4" /> : <ChevronRight className="w-4 h-4" />}
                        </button>
                      </td>
                      <td className="px-4 py-3 font-medium text-slate-800">{m.name}</td>
                      <td className="px-4 py-3 text-slate-500">{m.category}</td>
                      <td className="px-4 py-3 text-slate-500">{m.manufacturer || "—"}</td>
                      <td className="px-4 py-3 text-right font-mono-data">{money(m.unitPrice)}</td>
                      <td className="px-4 py-3 text-right">
                        <span className={cx(
                          "inline-flex px-2.5 py-0.5 rounded-full text-xs font-semibold font-mono-data",
                          m.totalAvailable < 20 ? "bg-amber-100 text-amber-800" : "bg-emerald-100 text-emerald-800"
                        )}>
                          {m.totalAvailable} {m.unit}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-center">
                        {m.prescriptionRequired ? (
                          <span className="inline-flex px-2 py-0.5 rounded-full text-[10px] font-bold bg-red-100 text-red-700">Rx</span>
                        ) : (
                          <span className="text-slate-300">—</span>
                        )}
                      </td>
                      <td className="px-4 py-3 text-right whitespace-nowrap">
                        {canEdit && (
                          <button onClick={() => openEdit(m)} data-testid={`edit-medicine-${m.medicineId}`} className="text-slate-400 hover:text-[#0D9488] p-1">
                            <Pencil className="w-4 h-4" />
                          </button>
                        )}
                        {canDelete && (
                          <button onClick={() => remove(m)} data-testid={`delete-medicine-${m.medicineId}`} className="text-slate-400 hover:text-red-600 p-1">
                            <Trash2 className="w-4 h-4" />
                          </button>
                        )}
                      </td>
                    </tr>
                    {expanded === m.medicineId && (
                      <tr className="bg-slate-50/80">
                        <td />
                        <td colSpan={7} className="px-4 py-3">
                          <div className="flex items-center gap-2 text-xs font-semibold text-slate-500 mb-2">
                            <Layers className="w-3.5 h-3.5" /> Stock Batches (FIFO by expiry)
                          </div>
                          {batches.length === 0 ? (
                            <div className="text-xs text-slate-400 py-2">No batches for this medicine.</div>
                          ) : (
                            <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
                              {batches.map((b) => (
                                <div key={b.batchId} className="bg-white border border-slate-200 rounded-lg px-3 py-2" data-testid={`batch-card-${b.batchId}`}>
                                  <div className="flex items-center justify-between">
                                    <span className="font-mono-data text-xs font-semibold text-slate-700">{b.batchNo}</span>
                                    <span className="font-mono-data text-xs font-bold text-[#0D9488]">{b.quantityAvailable}</span>
                                  </div>
                                  <div className="text-[11px] text-slate-400 mt-1">Expiry: {b.expiryDate}</div>
                                </div>
                              ))}
                            </div>
                          )}
                        </td>
                      </tr>
                    )}
                  </React.Fragment>
                ))}
              </tbody>
            </table>
          )}
        </Card>
      </div>

      <Modal open={open} onClose={() => setOpen(false)} title={editing ? "Edit Medicine" : "Add Medicine"}>
        <form onSubmit={save} className="space-y-4">
          <Field label="Name">
            <TextInput data-testid="medicine-name-input" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
          </Field>
          <div className="grid grid-cols-2 gap-4">
            <Field label="Category">
              <TextInput data-testid="medicine-category-input" value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })} required />
            </Field>
            <Field label="Manufacturer">
              <TextInput value={form.manufacturer} onChange={(e) => setForm({ ...form, manufacturer: e.target.value })} />
            </Field>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <Field label="Unit">
              <Select data-testid="medicine-unit-select" value={form.unit} onChange={(e) => setForm({ ...form, unit: e.target.value })}>
                {["tablet", "capsule", "bottle", "vial", "strip", "sachet", "tube"].map((u) => (
                  <option key={u} value={u}>{u}</option>
                ))}
              </Select>
            </Field>
            <Field label="Unit Price (₹)">
              <TextInput data-testid="medicine-price-input" type="number" step="0.01" min="0" value={form.unitPrice} onChange={(e) => setForm({ ...form, unitPrice: e.target.value })} required />
            </Field>
          </div>
          <label className="flex items-center gap-3 cursor-pointer">
            <input
              type="checkbox"
              data-testid="medicine-rx-checkbox"
              checked={form.prescriptionRequired}
              onChange={(e) => setForm({ ...form, prescriptionRequired: e.target.checked })}
              className="w-4 h-4 accent-[#0D9488]"
            />
            <span className="text-sm text-slate-700">Prescription required to sell</span>
          </label>
          <div className="flex justify-end gap-2 pt-2">
            <Btn type="button" variant="outline" onClick={() => setOpen(false)}>Cancel</Btn>
            <Btn type="submit" data-testid="medicine-save-button" disabled={saving}>{saving ? "Saving…" : "Save"}</Btn>
          </div>
        </form>
      </Modal>
    </Layout>
  );
}
