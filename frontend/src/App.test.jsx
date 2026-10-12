import React from "react";
import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import {
  render,
  screen,
  fireEvent,
  waitFor,
  cleanup,
} from "@testing-library/react";
import "@testing-library/jest-dom/vitest";
import { App } from "./App";
import { request } from "./api";
vi.mock("./api", () => ({ request: vi.fn() }));
const shoe = {
  id: 1,
  name: "Velocity Runner",
  category: "Running",
  price: 89,
  stock: 2,
  active: true,
  description: "Run comfortably",
};
let admin = false;
beforeEach(() => {
  admin = false;
  request.mockReset();
  request.mockImplementation(async (path) => {
    if (path === "/me") {
      if (admin)
        return {
          name: "Admin",
          email: "admin@sportyshoes.local",
          role: "ADMIN",
        };
      throw new Error("Sign in required");
    }
    if (path.includes("products")) return [shoe];
    if (path.includes("/reports"))
      return [
        {
          id: 1,
          date: "2026-10-12",
          customer: "runner@example.com",
          product: shoe.name,
          category: "Running",
          quantity: 1,
          total: 89,
        },
      ];
    if (path.includes("/users"))
      return [
        {
          id: 1,
          name: "Runner",
          email: "runner@example.com",
          role: "USER",
          createDate: "2026-10-12",
        },
      ];
    return null;
  });
});
afterEach(cleanup);
describe("store workflows", () => {
  it("renders catalogue and routes anonymous checkout to login", async () => {
    render(<App />);
    await screen.findByText("Velocity Runner");
    fireEvent.click(screen.getByRole("button", { name: "Buy one pair" }));
    expect(
      screen.getByRole("heading", { name: "Welcome back" }),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "Reports" }),
    ).not.toBeInTheDocument();
  });
  it("shows request failures to the user", async () => {
    request.mockRejectedValue(new Error("Service unavailable"));
    render(<App />);
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Service unavailable",
    );
  });
  it("registers then authenticates using normalized email", async () => {
    render(<App />);
    await screen.findByText("Velocity Runner");
    await waitFor(() =>
      expect(screen.queryByText("Loading…")).not.toBeInTheDocument(),
    );
    fireEvent.click(screen.getByRole("button", { name: "Sign in" }));
    fireEvent.click(screen.getByRole("button", { name: "Create an account" }));
    fireEvent.change(screen.getByLabelText("Name"), {
      target: { value: "Jonas" },
    });
    fireEvent.change(screen.getByLabelText("Email"), {
      target: { value: "JONAS@example.com" },
    });
    fireEvent.change(screen.getByLabelText("Password"), {
      target: { value: "Password123!" },
    });
    await waitFor(() =>
      expect(
        screen.getByRole("button", { name: "Register and sign in" }),
      ).toBeEnabled(),
    );
    fireEvent.click(
      screen.getByRole("button", { name: "Register and sign in" }),
    );
    await waitFor(() =>
      expect(request).toHaveBeenCalledWith(
        "/register",
        expect.objectContaining({ method: "POST" }),
      ),
    );
    await waitFor(() =>
      expect(request).toHaveBeenCalledWith(
        "/login",
        expect.objectContaining({ method: "POST" }),
      ),
    );
    const call = request.mock.calls.find(([p]) => p === "/login");
    expect(call[1].body.get("username")).toBe("jonas@example.com");
  });
  it("creates and archives products from admin controls", async () => {
    admin = true;
    render(<App />);
    fireEvent.click(await screen.findByRole("button", { name: "Products" }));
    await screen.findByRole("button", { name: "Edit" });
    fireEvent.click(screen.getByRole("button", { name: "Add product" }));
    fireEvent.change(screen.getByLabelText("name"), {
      target: { value: "New Shoe" },
    });
    fireEvent.change(screen.getByLabelText("price"), {
      target: { value: "95.50" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Save product" }));
    await waitFor(() =>
      expect(request).toHaveBeenCalledWith(
        "/admin/products",
        expect.objectContaining({
          method: "POST",
          body: expect.objectContaining({ name: "New Shoe", price: 95.5 }),
        }),
      ),
    );
    await screen.findByText("Product saved.");
    fireEvent.click(screen.getByRole("button", { name: "Archive" }));
    await waitFor(() =>
      expect(request).toHaveBeenCalledWith("/admin/products/1", {
        method: "DELETE",
      }),
    );
  });
  it("applies inclusive dates and category to report request", async () => {
    admin = true;
    render(<App />);
    fireEvent.click(await screen.findByRole("button", { name: "Reports" }));
    await screen.findByText(/1 purchases/);
    fireEvent.change(screen.getByLabelText("From (inclusive)"), {
      target: { value: "2026-10-01" },
    });
    fireEvent.change(screen.getByLabelText("To (inclusive)"), {
      target: { value: "2026-10-12" },
    });
    fireEvent.change(screen.getByLabelText("Category"), {
      target: { value: "Running" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Apply filters" }));
    await waitFor(() =>
      expect(request).toHaveBeenCalledWith(
        "/admin/reports?from=2026-10-01&to=2026-10-12&category=Running",
      ),
    );
  });
  it("searches registered users", async () => {
    admin = true;
    render(<App />);
    fireEvent.click(await screen.findByRole("button", { name: "Users" }));
    await screen.findByText("runner@example.com");
    fireEvent.change(screen.getByLabelText("Search by name or email"), {
      target: { value: "runner" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Search" }));
    await waitFor(() =>
      expect(request).toHaveBeenCalledWith("/admin/users?search=runner"),
    );
  });
});
