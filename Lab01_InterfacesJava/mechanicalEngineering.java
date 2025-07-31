
class MechanicalEngineering implements AcademicProgram {
    private String name;
    private int yearsDuration;
    private String coordinator;
    private int capacity;

    public MechanicalEngineering(String name, int yearsDuration, String coordinator, int capacity) {
        this.name = name;
        this.yearsDuration = yearsDuration;
        this.coordinator = coordinator;
        this.capacity = capacity;
    }

    @Override
    public void programDescription() {
        System.out.println("Program: " + name + ", focused on mechanical design.");
    }

    @Override
    public void duration() {
        System.out.println("Duration: " + yearsDuration + " years.");
    }

    @Override
    public void requirements() {
        System.out.println("Requirements: Advanced math and physics.");
    }

    @Override
    public void showCoordinator() {
        System.out.println("Coordinator: " + coordinator);
    }

    @Override
    public void updateCoordinator(String newCoordinator) {
        this.coordinator = newCoordinator;
        System.out.println("Coordinator updated to: " + coordinator);
    }

    @Override
    public void showCapacity() {
        System.out.println("Available capacity: " + capacity);
    }

    @Override
    public void updateCapacity(int newCapacity) {
        this.capacity = newCapacity;
        System.out.println("Capacity updated to: " + capacity);
    }

    @Override
    public void checkStatus() {
        System.out.println("Status of program " + name + ": Active.");
    }

    public void designMachines() {
        System.out.println("Students of " + name + " design machines.");
    }

    public void conductProjects() {
        System.out.println("Students of " + name + " conduct mechanical projects.");
    }
}