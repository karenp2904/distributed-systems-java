
class Student implements Person {
    private String name;
    private int age;
    private String status;

    public Student(String name, int age) {
        this.name = name;
        this.age = age;
        this.status = "Active";
    }

    @Override
    public void displayInformation() {
        System.out.println("Student: " + name + ", Age: " + age + ", Status: " + status);
    }

    @Override
    public void greet() {
        System.out.println("Hello! I am " + name + ", a student.");
    }

    @Override
    public void sayGoodbye() {
        System.out.println("Goodbye, see you in class.");
    }

    @Override
    public void changeName(String newName) {
        this.name = newName;
        System.out.println("Name changed to: " + name);
    }

    @Override
    public void showAge() {
        System.out.println("Age: " + age);
    }

    @Override
    public void updateAge(int newAge) {
        this.age = newAge;
        System.out.println("Age updated to: " + age);
    }

    @Override
    public void showStatus() {
        System.out.println("Status: " + status);
    }

    @Override
    public void setStatus(String status) {
        this.status = status;
        System.out.println("Status changed to: " + status);
    }

    public void study() {
        System.out.println(name + " is studying.");
    }

    public void attendClass() {
        System.out.println(name + " is attending class.");
    }
}