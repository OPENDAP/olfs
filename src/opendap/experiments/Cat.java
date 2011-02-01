package opendap.experiments;

public class Cat extends Animal {

    public static void staticMethod() {
        System.out.println("The staticMethod in Cat.");
    }

    @Override
    public void testInstanceMethod() {
        System.out.println("The instance method in Cat.");
    }

    public static void main(String[] args) {
        Cat myCat = new Cat();
        Animal myAnimal = myCat;

        System.out.println("Animal class:");
        Animal.staticMethod();


        System.out.println("\n\nAnimal class instance: ");
        myAnimal.staticMethod();
        myAnimal.testInstanceMethod();

        System.out.println("\n\nCat class instance: ");
        myCat.staticMethod();
        myCat.testInstanceMethod();


    }
}
