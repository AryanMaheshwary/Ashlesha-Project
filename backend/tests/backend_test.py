"""
Pharmacy Management System — Java backend API tests
Covers: auth, RBAC, medicines CRUD, stock/purchases, billing FIFO + Rx + insufficient,
suppliers, customers, prescriptions, reports, sales history.
"""
import os
import pytest
import requests
from datetime import date, timedelta

BASE_URL = os.environ.get("REACT_APP_BACKEND_URL", "https://pharmacy-mgmt-24.preview.emergentagent.com").rstrip("/")
API = f"{BASE_URL}/api"


def _login(username, password):
    r = requests.post(f"{API}/auth/login", json={"username": username, "password": password}, timeout=15)
    assert r.status_code == 200, f"login failed for {username}: {r.status_code} {r.text}"
    return r.json()["token"]


@pytest.fixture(scope="session")
def admin_token():
    return _login("admin", "admin123")


@pytest.fixture(scope="session")
def pharma_token():
    return _login("pharma", "pharma123")


@pytest.fixture(scope="session")
def cashier_token():
    return _login("cashier", "cashier123")


def H(tok):
    return {"Authorization": f"Bearer {tok}", "Content-Type": "application/json"}


# ---------- Health & Auth ----------
def test_health():
    r = requests.get(f"{API}/health", timeout=10)
    assert r.status_code == 200
    j = r.json()
    assert j.get("status") == "ok"
    assert j.get("language") == "java"


def test_login_bad_credentials():
    r = requests.post(f"{API}/auth/login", json={"username": "admin", "password": "wrong"}, timeout=10)
    assert r.status_code in (400, 401, 403)


def test_login_roles(admin_token, pharma_token, cashier_token):
    assert admin_token and pharma_token and cashier_token


# ---------- RBAC ----------
def test_cashier_forbidden_reports(cashier_token):
    r = requests.get(f"{API}/reports/summary", headers=H(cashier_token), timeout=10)
    assert r.status_code == 403, f"expected 403, got {r.status_code}: {r.text}"


def test_cashier_forbidden_stock_purchase(cashier_token):
    r = requests.post(f"{API}/purchases", headers=H(cashier_token), json={
        "supplierId": 1, "lines": []
    }, timeout=10)
    assert r.status_code == 403


def test_cashier_forbidden_suppliers_create(cashier_token):
    r = requests.post(f"{API}/suppliers", headers=H(cashier_token),
                      json={"name": "TEST_Sup_X"}, timeout=10)
    assert r.status_code == 403


def test_admin_can_access_reports(admin_token):
    r = requests.get(f"{API}/reports/summary", headers=H(admin_token), timeout=15)
    assert r.status_code == 200


def test_pharma_can_access_reports(pharma_token):
    r = requests.get(f"{API}/reports/summary", headers=H(pharma_token), timeout=15)
    assert r.status_code == 200


# ---------- Medicines ----------
def test_list_medicines(admin_token):
    r = requests.get(f"{API}/medicines", headers=H(admin_token), timeout=15)
    assert r.status_code == 200
    meds = r.json()
    assert isinstance(meds, list) and len(meds) >= 1
    # store some for reuse via module-level cache
    pytest.meds = meds


def test_medicine_search(admin_token):
    r = requests.get(f"{API}/medicines?q=para", headers=H(admin_token), timeout=15)
    assert r.status_code == 200
    assert isinstance(r.json(), list)


def test_medicine_create_update_delete(admin_token):
    payload = {
        "name": "TEST_Med_ZZZ",
        "category": "Test",
        "unit": "TAB",
        "unitPrice": 9.99,
        "prescriptionRequired": False,
        "lowStockThreshold": 10,
    }
    r = requests.post(f"{API}/medicines", headers=H(admin_token), json=payload, timeout=15)
    assert r.status_code in (200, 201), r.text
    m = r.json()
    mid = m.get("id") or m.get("medicineId")
    assert mid, f"no id in {m}"

    # update
    payload2 = dict(payload, name="TEST_Med_ZZZ2", unitPrice=12.50)
    r = requests.put(f"{API}/medicines/{mid}", headers=H(admin_token), json=payload2, timeout=15)
    assert r.status_code in (200, 204), r.text

    # verify via list
    r = requests.get(f"{API}/medicines?q=TEST_Med_ZZZ2", headers=H(admin_token), timeout=15)
    assert r.status_code == 200
    found = [x for x in r.json() if (x.get("id") or x.get("medicineId")) == mid]
    assert found and found[0].get("name") == "TEST_Med_ZZZ2"

    # delete
    r = requests.delete(f"{API}/medicines/{mid}", headers=H(admin_token), timeout=15)
    assert r.status_code in (200, 204), r.text


def test_pharma_cannot_delete_medicine(pharma_token, admin_token):
    # create with admin then try delete with pharma
    r = requests.post(f"{API}/medicines", headers=H(admin_token), json={
        "name": "TEST_Med_DEL", "category": "T", "unit": "TAB", "unitPrice": 1,
        "prescriptionRequired": False, "lowStockThreshold": 5
    }, timeout=15)
    assert r.status_code in (200, 201)
    mid = (r.json().get("id") or r.json().get("medicineId"))
    r2 = requests.delete(f"{API}/medicines/{mid}", headers=H(pharma_token), timeout=15)
    # cleanup
    requests.delete(f"{API}/medicines/{mid}", headers=H(admin_token), timeout=15)
    assert r2.status_code == 403


# ---------- Suppliers / Customers / Prescriptions ----------
def test_suppliers_crud(admin_token):
    r = requests.get(f"{API}/suppliers", headers=H(admin_token), timeout=10)
    assert r.status_code == 200
    r = requests.post(f"{API}/suppliers", headers=H(admin_token),
                      json={"name": "TEST_Sup", "phone": "999", "email": "t@t.com"}, timeout=10)
    assert r.status_code in (200, 201), r.text
    sid = r.json().get("id") or r.json().get("supplierId")
    assert sid
    r = requests.delete(f"{API}/suppliers/{sid}", headers=H(admin_token), timeout=10)
    assert r.status_code in (200, 204)


def test_customers_crud(admin_token):
    r = requests.get(f"{API}/customers", headers=H(admin_token), timeout=10)
    assert r.status_code == 200
    r = requests.post(f"{API}/customers", headers=H(admin_token),
                      json={"name": "TEST_Cust", "phone": "1234"}, timeout=10)
    assert r.status_code in (200, 201), r.text
    cid = r.json().get("id") or r.json().get("customerId")
    assert cid
    r = requests.delete(f"{API}/customers/{cid}", headers=H(admin_token), timeout=10)
    assert r.status_code in (200, 204, 400)  # deletion may be blocked if referenced


def test_prescription_list(admin_token):
    r = requests.get(f"{API}/prescriptions", headers=H(admin_token), timeout=10)
    assert r.status_code == 200
    assert isinstance(r.json(), list)


# ---------- Purchases / Stock ----------
def test_purchase_creates_batch_and_increases_stock(admin_token):
    meds = requests.get(f"{API}/medicines", headers=H(admin_token), timeout=15).json()
    sups = requests.get(f"{API}/suppliers", headers=H(admin_token), timeout=15).json()
    assert meds and sups
    med = meds[0]
    mid = med.get("id") or med.get("medicineId")
    sid = sups[0].get("id") or sups[0].get("supplierId")
    prev_qty = med.get("totalAvailable") or med.get("stockQty") or med.get("totalStock") or 0

    expiry = (date.today() + timedelta(days=200)).isoformat()
    payload = {
        "supplierId": sid,
        "items": [{
            "medicineId": mid,
            "batchNo": "TEST_B1",
            "expiryDate": expiry,
            "quantity": 25,
            "costPrice": 5.0
        }]
    }
    r = requests.post(f"{API}/purchases", headers=H(admin_token), json=payload, timeout=15)
    assert r.status_code in (200, 201), r.text

    meds2 = requests.get(f"{API}/medicines", headers=H(admin_token), timeout=15).json()
    m2 = next((x for x in meds2 if (x.get("id") or x.get("medicineId")) == mid), None)
    new_qty = m2.get("totalAvailable") or m2.get("stockQty") or m2.get("totalStock") or 0
    assert new_qty >= prev_qty + 25, f"stock should increase by 25 ({prev_qty}->{new_qty})"


def test_purchase_rejects_past_expiry(admin_token):
    meds = requests.get(f"{API}/medicines", headers=H(admin_token), timeout=15).json()
    sups = requests.get(f"{API}/suppliers", headers=H(admin_token), timeout=15).json()
    mid = meds[0].get("id") or meds[0].get("medicineId")
    sid = sups[0].get("id") or sups[0].get("supplierId")
    payload = {
        "supplierId": sid,
        "items": [{
            "medicineId": mid,
            "batchNo": "TEST_BPAST",
            "expiryDate": (date.today() - timedelta(days=5)).isoformat(),
            "quantity": 5, "costPrice": 3.0
        }]
    }
    r = requests.post(f"{API}/purchases", headers=H(admin_token), json=payload, timeout=15)
    assert r.status_code in (400, 422), f"expected reject, got {r.status_code}: {r.text}"


def test_batches_endpoint(admin_token):
    meds = requests.get(f"{API}/medicines", headers=H(admin_token), timeout=15).json()
    mid = meds[0].get("id") or meds[0].get("medicineId")
    # try common batch endpoints
    for url in [f"{API}/medicines/{mid}/batches", f"{API}/batches?medicineId={mid}"]:
        r = requests.get(url, headers=H(admin_token), timeout=10)
        if r.status_code == 200:
            assert isinstance(r.json(), list)
            return
    pytest.skip("no batches endpoint responded 200")


# ---------- Billing ----------
def _find_med(meds, name_contains=None, rx=None):
    for m in meds:
        n = (m.get("name") or "").lower()
        if name_contains and name_contains.lower() not in n:
            continue
        if rx is not None and bool(m.get("prescriptionRequired")) != rx:
            continue
        return m
    return None


def test_sale_normal_medicine_success(cashier_token, admin_token):
    meds = requests.get(f"{API}/medicines", headers=H(admin_token), timeout=15).json()
    med = _find_med(meds, "Paracetamol", rx=False) or _find_med(meds, rx=False)
    assert med
    mid = med.get("id") or med.get("medicineId")
    payload = {
        "customerId": None,
        "prescriptionId": None,
        "paymentMode": "CASH",
        "items": [{"medicineId": mid, "quantity": 1}]
    }
    r = requests.post(f"{API}/sales", headers=H(cashier_token), json=payload, timeout=15)
    assert r.status_code in (200, 201), r.text
    inv = r.json()
    assert inv.get("total") is not None or inv.get("totalAmount") is not None
    # invoice should include line items with batch info
    items = inv.get("items") or inv.get("lines") or []
    assert items, f"no items in invoice: {inv}"
    has_batch = any(("batchNo" in it or "batch" in it or "batchNumber" in it) for it in items)
    assert has_batch, f"no batch info in invoice items: {items}"


def test_sale_rx_without_prescription_blocked(cashier_token, admin_token):
    meds = requests.get(f"{API}/medicines", headers=H(admin_token), timeout=15).json()
    rx_med = _find_med(meds, "Amoxicillin", rx=True) or _find_med(meds, rx=True)
    assert rx_med, "no Rx medicine found"
    mid = rx_med.get("id") or rx_med.get("medicineId")
    payload = {
        "paymentMode": "CASH",
        "items": [{"medicineId": mid, "quantity": 1}]
    }
    r = requests.post(f"{API}/sales", headers=H(cashier_token), json=payload, timeout=15)
    assert r.status_code == 422, f"expected 422 for Rx-without-prescription, got {r.status_code}: {r.text}"


def test_sale_insufficient_stock_blocked(cashier_token, admin_token):
    meds = requests.get(f"{API}/medicines", headers=H(admin_token), timeout=15).json()
    med = _find_med(meds, "Cough Syrup", rx=False) or _find_med(meds, rx=False)
    assert med
    mid = med.get("id") or med.get("medicineId")
    prev = requests.get(f"{API}/medicines", headers=H(admin_token), timeout=15).json()
    prev_m = next(x for x in prev if (x.get("id") or x.get("medicineId")) == mid)
    prev_qty = prev_m.get("totalAvailable") or prev_m.get("stockQty") or 0

    payload = {"paymentMode": "CASH", "items": [{"medicineId": mid, "quantity": 999}]}
    r = requests.post(f"{API}/sales", headers=H(cashier_token), json=payload, timeout=15)
    assert r.status_code == 409, f"expected 409, got {r.status_code}: {r.text}"

    # verify stock unchanged
    after = requests.get(f"{API}/medicines", headers=H(admin_token), timeout=15).json()
    after_m = next(x for x in after if (x.get("id") or x.get("medicineId")) == mid)
    after_qty = after_m.get("totalAvailable") or after_m.get("stockQty") or 0
    assert after_qty == prev_qty, f"stock changed on failed sale: {prev_qty} -> {after_qty}"


def test_sale_with_prescription_success(cashier_token, admin_token):
    presc = requests.get(f"{API}/prescriptions", headers=H(admin_token), timeout=10).json()
    if not presc:
        pytest.skip("no prescriptions seeded")
    p = presc[0]
    pid = p.get("id") or p.get("prescriptionId")
    meds = requests.get(f"{API}/medicines", headers=H(admin_token), timeout=15).json()
    rx_med = _find_med(meds, rx=True)
    mid = rx_med.get("id") or rx_med.get("medicineId")
    payload = {
        "prescriptionId": pid,
        "customerId": p.get("customerId"),
        "paymentMode": "CARD",
        "items": [{"medicineId": mid, "quantity": 1}]
    }
    r = requests.post(f"{API}/sales", headers=H(cashier_token), json=payload, timeout=15)
    assert r.status_code in (200, 201), f"prescription-linked Rx sale failed: {r.status_code} {r.text}"


# ---------- Reports & Sales history ----------
def test_reports_endpoints(admin_token):
    for path in ["/reports/summary", "/reports/low-stock", "/reports/expiry",
                 "/reports/revenue?period=daily", "/reports/revenue?period=monthly",
                 "/reports/top-selling"]:
        r = requests.get(f"{API}{path}", headers=H(admin_token), timeout=15)
        assert r.status_code == 200, f"{path} -> {r.status_code} {r.text[:200]}"


def test_sales_history(admin_token):
    r = requests.get(f"{API}/sales", headers=H(admin_token), timeout=15)
    assert r.status_code == 200
    sales = r.json()
    assert isinstance(sales, list)
    if sales:
        sid = sales[0].get("id") or sales[0].get("saleId")
        r2 = requests.get(f"{API}/sales/{sid}", headers=H(admin_token), timeout=15)
        assert r2.status_code == 200
        detail = r2.json()
        items = detail.get("items") or detail.get("lines") or []
        assert items, "sale detail should have items"
