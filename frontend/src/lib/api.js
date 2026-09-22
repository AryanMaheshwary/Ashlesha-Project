import axios from "axios";

const API = `${process.env.REACT_APP_BACKEND_URL}/api`;

const client = axios.create({ baseURL: API });

client.interceptors.request.use((cfg) => {
  const token = localStorage.getItem("pms_token");
  if (token) cfg.headers.Authorization = `Bearer ${token}`;
  return cfg;
});

export function apiError(e) {
  return e?.response?.data?.error || e?.message || "Something went wrong";
}

export const api = {
  // auth
  login: (username, password) => client.post("/auth/login", { username, password }),
  me: () => client.get("/auth/me"),
  logout: () => client.post("/auth/logout"),

  // medicines
  medicines: (search) => client.get("/medicines", { params: { search } }),
  createMedicine: (m) => client.post("/medicines", m),
  updateMedicine: (id, m) => client.put(`/medicines/${id}`, m),
  deleteMedicine: (id) => client.delete(`/medicines/${id}`),

  // stock / purchases
  batches: (medicineId) => client.get("/batches", { params: { medicineId } }),
  purchases: () => client.get("/purchases"),
  createPurchase: (p) => client.post("/purchases", p),

  // suppliers
  suppliers: () => client.get("/suppliers"),
  createSupplier: (s) => client.post("/suppliers", s),
  updateSupplier: (id, s) => client.put(`/suppliers/${id}`, s),
  deleteSupplier: (id) => client.delete(`/suppliers/${id}`),

  // customers
  customers: () => client.get("/customers"),
  createCustomer: (c) => client.post("/customers", c),
  updateCustomer: (id, c) => client.put(`/customers/${id}`, c),
  deleteCustomer: (id) => client.delete(`/customers/${id}`),

  // prescriptions
  prescriptions: () => client.get("/prescriptions"),
  createPrescription: (p) => client.post("/prescriptions", p),

  // sales
  sales: () => client.get("/sales"),
  sale: (id) => client.get(`/sales/${id}`),
  createSale: (s) => client.post("/sales", s),

  // reports
  reportSummary: () => client.get("/reports/summary"),
  reportLowStock: (threshold) => client.get("/reports/low-stock", { params: { threshold } }),
  reportExpiry: (days) => client.get("/reports/expiry", { params: { days } }),
  reportRevenue: (period) => client.get("/reports/revenue", { params: { period } }),
  reportTopSelling: (limit) => client.get("/reports/top-selling", { params: { limit } }),
};
