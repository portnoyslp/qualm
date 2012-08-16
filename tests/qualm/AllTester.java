package qualm;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses( { CueTest.class, EventMapperTest.class, EventTemplateTest.class })
public class AllTester { }
