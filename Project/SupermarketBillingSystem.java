import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;
import java.util.Stack;

public class SupermarketBillingSystem {

    private static final Scanner sc = new Scanner(System.in);

    // Queue: customers are billed in the order in which they arrive.
    private static final Queue<String> billingQueue = new LinkedList<>();

    // HashMap: stores product names and their fixed prices.
    private static final HashMap<String, Integer> priceList = new HashMap<>();

    // ArrayList: keeps previously saved bills and bills created in this run.
    private static final ArrayList<String> billHistory = new ArrayList<>();

    private static final String BILL_FILE = "bills.txt";

    // Represents one scanned product entry. Each entry can be undone.
    private static class BillItem {
        String name;
        int quantity;
        int amount;

        BillItem(String name, int quantity, int unitPrice) {
            this.name = name;
            this.quantity = quantity;
            this.amount = quantity * unitPrice;
        }
    }

    public static void main(String[] args) {
        initializePriceList();
        loadBillsFromFile();

        System.out.println("==================================");
        System.out.println("       SUPERMARKET BILLING");
        System.out.println("==================================");

        int choice;

        do {
            displayMenu();
            choice = readInteger("Enter your choice: ");

            switch (choice) {
                case 1:
                    addCustomer();
                    break;

                case 2:
                    billNextCustomer();
                    break;

                case 3:
                    viewBillingQueue();
                    break;

                case 4:
                    saveBillsToFile();
                    System.out.println("Bills saved successfully.");
                    System.out.println("Thank you for using the Supermarket Billing System!");
                    break;

                default:
                    System.out.println("Invalid menu option. Please try again.");
            }
        } while (choice != 4);

        sc.close();
    }

    // Adds the products and their prices to the HashMap.
    private static void initializePriceList() {
        // Arrays store the product names and their corresponding prices.
        String[] products = {"Rice", "Milk", "Bread"};
        int[] prices = {60, 30, 40};

        // Transfer the array data into the HashMap for price lookup.
        for (int i = 0; i < products.length; i++) {
            priceList.put(products[i].toLowerCase(), prices[i]);
        }
    }

    // Displays the main menu.
    private static void displayMenu() {
        System.out.println("\n----------- MAIN MENU -----------");
        System.out.println("1. Add Customer");
        System.out.println("2. Bill Next Customer");
        System.out.println("3. View Billing Queue");
        System.out.println("4. Exit");
    }

    // Reads an integer safely without crashing on invalid input.
    private static int readInteger(String message) {
        while (true) {
            System.out.print(message);
            String input = sc.nextLine().trim();

            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }

    // Adds a customer at the rear of the queue.
    private static void addCustomer() {
        System.out.print("Enter customer name: ");
        String name = sc.nextLine().trim();

        if (name.isEmpty()) {
            System.out.println("Customer name cannot be empty.");
            return;
        }

        billingQueue.offer(name);
        System.out.println(name + " added to billing queue. Position: "
                + billingQueue.size());
    }

    // Bills the customer at the front of the queue.
    private static void billNextCustomer() {
        if (billingQueue.isEmpty()) {
            System.out.println("Billing queue is empty. No customer to bill.");
            return;
        }

        String customerName = billingQueue.peek();
        Stack<BillItem> undoStack = new Stack<>();

        System.out.println("\nBilling customer: " + customerName);
        System.out.println("Available items: Rice (60), Milk (30), Bread (40)");
        System.out.println("Enter an item name, 'Undo' to remove the last item,");
        System.out.println("or 'Done' to finish billing.");

        while (true) {
            System.out.print("\nEnter item name (or Undo/Done): ");
            String itemName = sc.nextLine().trim();

            if (itemName.equalsIgnoreCase("Done")) {
                break;
            }

            if (itemName.equalsIgnoreCase("Undo")) {
                undoLastItem(undoStack);
                continue;
            }

            if (itemName.isEmpty()) {
                System.out.println("Item name cannot be empty.");
                continue;
            }

            String key = itemName.toLowerCase();

            if (!priceList.containsKey(key)) {
                System.out.println("Item " + itemName + " not found in price list.");
                continue;
            }

            int quantity = readInteger("Enter quantity: ");

            if (quantity <= 0) {
                System.out.println("Quantity must be greater than zero.");
                continue;
            }

            int unitPrice = priceList.get(key);
            undoStack.push(new BillItem(toTitleCase(key), quantity, unitPrice));

            System.out.println(toTitleCase(key) + " x" + quantity
                    + " added. Amount: " + (unitPrice * quantity));
        }

        // Generate and save the bill after the customer finishes.
        String bill = generateBill(customerName, undoStack);
        System.out.println(bill);
        billHistory.add(bill);

        // Remove the billed customer from the front of the queue.
        billingQueue.poll();

        if (billingQueue.isEmpty()) {
            System.out.println("Queue is now empty.");
        }
    }

    // Removes the most recently scanned product using the Stack (LIFO).
    private static void undoLastItem(Stack<BillItem> undoStack) {
        if (undoStack.isEmpty()) {
            System.out.println("No item available to undo.");
            return;
        }

        BillItem removedItem = undoStack.pop();
        System.out.println("Last item " + removedItem.name + " removed.");
    }

    // Creates the bill using the items remaining in the Stack.
    private static String generateBill(String customerName,
                                       Stack<BillItem> undoStack) {
        int total = 0;
        StringBuilder details = new StringBuilder();

        for (BillItem item : undoStack) {
            if (details.length() > 0) {
                details.append(", ");
            }

            details.append(item.name)
                    .append(" x")
                    .append(item.quantity)
                    .append(" = ")
                    .append(item.amount);

            total += item.amount;
        }

        if (details.length() == 0) {
            return "Bill for " + customerName + ": No items | Total 0";
        }

        return "Bill for " + customerName + ": " + details
                + " | Total " + total;
    }

    // Displays all customers currently waiting in the queue.
    private static void viewBillingQueue() {
        if (billingQueue.isEmpty()) {
            System.out.println("Billing queue is empty.");
            return;
        }

        System.out.println("\nCurrent Billing Queue:");
        int position = 1;

        for (String customer : billingQueue) {
            System.out.println(position + ". " + customer);
            position++;
        }
    }

    // Loads previously saved bills when the application starts.
    private static void loadBillsFromFile() {
        Path filePath = Paths.get(BILL_FILE);

        if (!Files.exists(filePath)) {
            return;
        }

        try {
            List<String> savedBills =
                    Files.readAllLines(filePath, StandardCharsets.UTF_8);
            billHistory.addAll(savedBills);
        } catch (IOException e) {
            System.out.println("Could not load previous bills: " + e.getMessage());
        }
    }

    // Saves all previous and newly generated bills when the application exits.
    private static void saveBillsToFile() {
        try {
            Files.write(Paths.get(BILL_FILE), billHistory,
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.out.println("Error saving bills: " + e.getMessage());
        }
    }

    // Converts a product name to title case for display.
    private static String toTitleCase(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        return value.substring(0, 1).toUpperCase()
                + value.substring(1).toLowerCase();
    }
}