package cloud.foundry.cli.crosscutting.beans;

/**
 * TODO doc
 */
public interface BeanVisitor {

    /**
     * TODO doc
     */
    void visit(GetAllBean getAllBean);

    /**
     * TODO doc
     */
    void visit(ApplicationBean applicationBean);

    /**
     * TODO doc
     */
    void visit(ApplicationManifestBean applicationManifestBean);

    /**
     * TODO doc
     */
    void visit(ServiceBean serviceBean);

    /**
     * TODO doc
     */
    void visit(SpaceDevelopersBean spaceDevelopersBean);

}
