package cloud.foundry.cli.services.dump;

import cloud.foundry.cli.crosscutting.mapping.beans.SpecBean;
import picocli.CommandLine;

@CommandLine.Command(name = "services", description = "TODO")
class DumpServicesCommand extends AbstractDumpCommand<SpecBean> {

    /**
     * TODO
     */
    protected DumpServicesCommand() {
        super(SpecBean.class, DumpServicesCommand.class);
    }
}