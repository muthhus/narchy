package com.github.disc99.injector;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.Test;

public class InjectorTest {

    @Test
    public void testNamedInjection() {
        Injector injector = new Injector();
        Controller controller = injector.getInstance(Controller.class);
        String actual = controller.index();
        assertThat(actual, is("2000-01-01"));
    }

    @Test
    public void testConfigInjection() {
        Injector injector = new Injector(new ClassDependencies()
                .bind(Logic.class, CountLogic.class)
                .bind(Task.class, CancelTask.class));
        Batch batch = injector.getInstance(Batch.class);
        String actual = batch.exe();
        assertThat(actual, is("10-Complete"));
    }
}

class Controller {
    @Inject
    Service service;

    String index() {
        return service.exe();
    }
}

interface Service {
    String exe();
}

@Named
class CalcService implements Service {
    @Inject
    private Dao dao;
    @Inject
    private Converter converter;

    @Override
    public String exe() {
        String date = dao.select();
        return converter.convert(date);
    }
}

interface Dao {
    String select();
}

@Named
class CountDao implements Dao {
    @Override
    public String select() {
        return "2000.01.01";
    }
}

interface Converter {
    String convert(String str);
}

@Named
class DotConverter implements Converter {
    @Override
    public String convert(String str) {
        return str.replace(".", "-");
    }
}

class Batch {
    @Inject
    private Logic logic;
    @Inject
    private Task task;

    public String exe() {
        return logic.num() + "-" + task.run();
    }
}

interface Logic {
    public int num();
}

class CountLogic implements Logic {
    @Override
    public int num() {
        return 10;
    }
}

interface Task {
    public String run();
}

class CancelTask implements Task {

    @Override
    public String run() {
        return "Complete";
    }

}
