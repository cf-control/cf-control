package cloud.foundry.cli.services.dump;

import cloud.foundry.cli.crosscutting.mapping.beans.SpecBean;
import picocli.CommandLine;

@CommandLine.Command(name = "space-developers", description = "TODO")
class DumpSpaceDevelopersCommand extends AbstractDumpCommand<SpecBean> {

    /**
     * TODO
     */
    protected DumpSpaceDevelopersCommand() {
        super(SpecBean.class, DumpSpaceDevelopersCommand.class);
    }
}