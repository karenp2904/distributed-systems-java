
class Teacher implements Person {
    private String name;
    private String subject;
    private String status;

    public Teacher(String name, String subject) {
        this.name = name;
        this.subject = subject;
        this.status = "Active";
    }

    @Override
    public void displayInformation() {
        System.out.println("Teacher: " + name + ", Subject: " + subject + ", Status: " + status);
    }

    @Override
    public void greet() {
        System.out.println("Hello! I am " + name + ", a teacher.");
    }

    @Override
    public void sayGoodbye() {
        System.out.println("See you later, good learning.");
    }

    @Override
    public void changeName(String newName) {
        this.name = newName;
        System.out.println("Name changed to: " + name);
    }

    @Override
    public void showAge() {
        System.out.println("Age not available for teachers.");
    }

    @Override
    public void updateAge(int newAge) {
        System.out.println("Age update not allowed for teachers.");
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

    public void teachClass() {
        System.out.println(name + " is teaching " + subject);
    }

    public void gradeExams() {
        System.out.println(name + " is grading exams.");
    }
}
