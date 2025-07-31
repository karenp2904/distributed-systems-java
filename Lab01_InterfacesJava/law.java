

class Law implements AcademicProgram {
    private String name;
    private int yearsDuration;
    private String coordinator;
    private int capacity;

    public Law(String name, int yearsDuration, String coordinator, int capacity) {
        this.name = name;
        this.yearsDuration = yearsDuration;
        this.coordinator = coordinator;
        this.capacity = capacity;
    }

    @Override
    public void programDescription() {
        System.out.println("Program: " + name + ", focused on law and justice.");
    }

    @Override
    public void duration() {
        System.out.println("Duration: " + yearsDuration + " years.");
    }

    @Override
    public void requirements() {
        System.out.println("Requirements: High school diploma and admission exam.");
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

    public void practiceLaw() {
        System.out.println("Students of " + name + " practice law.");
    }

    public void studyCases() {
        System.out.println("Students of " + name + " study legal cases.");
    }
}