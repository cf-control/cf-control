package cloud.foundry.cli.services.dump;

import cloud.foundry.cli.crosscutting.mapping.beans.SpecBean;
import picocli.CommandLine;

@CommandLine.Command(name = "applications", description = "TODO")
class DumpApplicationsCommand extends AbstractDumpCommand<SpecBean> {

    /**
     * TODO
     */
    protected DumpApplicationsCommand() {
        super(SpecBean.class, DumpApplicationsCommand.class);
    }
}
