public class Main {
    public static void main(String[] args) {
        // Test Person interface with Student
        Student student = new Student("Alice", 20);
        student.displayInformation();
        student.greet();
        student.study();
        student.updateAge(21);
        student.showAge();
        student.setStatus("On Break");
        student.showStatus();
        student.sayGoodbye();

        System.out.println(); // Blank line for readability

        // Test Person interface with Teacher
        Teacher teacher = new Teacher("Mr. Smith", "Physics");
        teacher.displayInformation();
        teacher.greet();
        teacher.teachClass();
        teacher.setStatus("Teaching");
        teacher.showStatus();
        teacher.sayGoodbye();

        System.out.println(); // Blank line for readability

        // Test AcademicProgram interface with Law
        Law law = new Law("Law", 5, "Dr. Jones", 50);
        law.programDescription();
        law.duration();
        law.requirements();
        law.showCoordinator();
        law.updateCoordinator("Dr. Brown");
        law.showCoordinator();
        law.showCapacity();
        law.updateCapacity(60);
        law.showCapacity();
        law.checkStatus();
        law.practiceLaw();

        System.out.println(); // Blank line for readability

        // Test AcademicProgram interface with MechanicalEngineering
        MechanicalEngineering mechEng = new MechanicalEngineering("Mechanical Engineering", 4, "Prof. Lee", 40);
        mechEng.programDescription();
        mechEng.duration();
        mechEng.requirements();
        mechEng.showCoordinator();
        mechEng.updateCoordinator("Prof. Kim");
        mechEng.showCoordinator();
        mechEng.showCapacity();
        mechEng.updateCapacity(45);
        mechEng.showCapacity();
        mechEng.checkStatus();
        mechEng.designMachines();
    }
}