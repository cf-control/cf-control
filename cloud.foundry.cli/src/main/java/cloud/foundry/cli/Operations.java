package cloud.foundry.cli;

public interface Operations {

    void create(Bean bean);

    void delete(Bean bean);

    void update(Bean bean);

    void get();
}
