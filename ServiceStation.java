import java.util.*;

public class ServiceStation {
    private final Queue<Car> queue;
    private final semaphore empty;
    private final semaphore full;
    private final semaphore mutex;
    private final semaphore pumps;
    private final int totalCars;
    private int servicedCars = 0;

    public ServiceStation(int waitingArea, int pumpCount, List<String> carOrder) {
        this.queue = new LinkedList<>();
        this.empty = new semaphore(waitingArea);
        this.full = new semaphore(0);
        this.mutex = new semaphore(1);
        this.pumps = new semaphore(pumpCount);
        this.totalCars = carOrder.size();

        for (int i = 1; i <= pumpCount; i++) {
            new Pump(i, queue, empty, full, mutex, pumps, this).start();
        }

        for (String name : carOrder) {
            new Car(name, queue, empty, full, mutex, pumps).start();
            try { Thread.sleep(300); } catch (InterruptedException ignored) {}
        }
    }

    public synchronized void carServiced() {
        servicedCars++;
        if (servicedCars == totalCars) {
            System.out.println("All cars processed; simulation ends");
            System.exit(0);
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.print("Waiting area capacity: ");
        int waiting = sc.nextInt();

        while (waiting < 1 || waiting > 10) {
            System.out.println("Invalid input! Waiting area must be between 1 and 10.");
            System.out.print("Enter waiting area capacity again: ");
            waiting = sc.nextInt();
        }

        System.out.print("Number of service bays (pumps): ");
        int pumpCount = sc.nextInt();
        sc.nextLine();

        System.out.print("Cars arriving (order): ");
        List<String> carOrder = Arrays.asList(sc.nextLine().trim().split(" "));

        new ServiceStation(waiting, pumpCount, carOrder);
    }
}

class semaphore {
    protected int value = 0;

    protected semaphore() { value = 0; }
    protected semaphore(int initial) { value = initial; }

    public synchronized void acquire() {
        value--;
        if (value < 0)
            try { wait(); } catch( InterruptedException e ) { }
    }

    public synchronized void release() {
        value++;
        if (value <= 0) notify();
    }

    public synchronized int availablePermits() {
        return value;
    }
}

class Car extends Thread {
    private final String name;
    private final Queue<Car> queue;
    private final semaphore empty, full, mutex, pumps;

    public Car(String name, Queue<Car> queue, semaphore empty, semaphore full, semaphore mutex, semaphore pumps) {
        this.name = name;
        this.queue = queue;
        this.empty = empty;
        this.full = full;
        this.mutex = mutex;
        this.pumps = pumps;
    }

    public String getCarName() { return name; }

    public void run() {
        System.out.println(name + " arrived");

        if (pumps.availablePermits() == 0) {
            System.out.println(name + " arrived and waiting");
        }

        empty.acquire();
        mutex.acquire();

        queue.add(this);

        mutex.release();
        full.release();
    }
}

class Pump extends Thread {
    private final int id;
    private final Queue<Car> queue;
    private final semaphore empty, full, mutex, pumps;
    private final ServiceStation station;

    public Pump(int id, Queue<Car> queue, semaphore empty, semaphore full, semaphore mutex, semaphore pumps, ServiceStation station) {
        this.id = id;
        this.queue = queue;
        this.empty = empty;
        this.full = full;
        this.mutex = mutex;
        this.pumps = pumps;
        this.station = station;
    }

    public void run() {
        while (true) {
            full.acquire();
            pumps.acquire();
            mutex.acquire();

            Car car = queue.remove();
            System.out.println("Pump " + id + ": " + car.getCarName() + " Occupied");
            System.out.println("Pump " + id + ": " + car.getCarName() + " login");
            System.out.println("Pump " + id + ": " + car.getCarName() + " begins service at Bay " + id);

            mutex.release();
            empty.release();

            try { Thread.sleep(2000); } catch (InterruptedException ignored) {}

            System.out.println("Pump " + id + ": " + car.getCarName() + " finishes service");
            System.out.println("Pump " + id + ": Bay " + id + " is now free");

            pumps.release();
            station.carServiced();
        }
    }
}
