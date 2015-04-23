package ut.com.isroot.stash.plugin;

import com.atlassian.stash.hook.HookResponse;
import com.atlassian.stash.hook.repository.RepositoryHookContext;
import com.atlassian.stash.repository.RefChange;
import com.atlassian.stash.repository.RefChangeType;
import com.atlassian.stash.repository.Repository;
import com.atlassian.stash.setting.Settings;
import com.google.common.collect.Lists;
import com.isroot.stash.plugin.YaccHook;
import com.isroot.stash.plugin.YaccService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.PrintWriter;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Sean Ford
 * @since 2014-01-15
 */
public class YaccHookTest
{
	@Mock private YaccService yaccService;
	@Mock private HookResponse hookResponse;
	@Mock private RepositoryHookContext repositoryHookContext;

	private YaccHook yaccHook;

	@Before
	public void setup()
	{
		MockitoAnnotations.initMocks(this);

		yaccHook = new YaccHook(yaccService);

		when(hookResponse.err()).thenReturn(mock(PrintWriter.class));
	}

	@Test
	public void testOnReceive_deleteRefChangesIgnored()
	{
		RefChange refChange = mock(RefChange.class);
		when(refChange.getType()).thenReturn(RefChangeType.DELETE);

	 	boolean allowed = yaccHook.onReceive(null, Lists.newArrayList(refChange), null);
		assertThat(allowed).isTrue();
		verifyZeroInteractions(yaccService);
	}

	@Test
	public void testOnReceive_pushRejectedIfThereAreErrors()
	{
		when(yaccService.checkRefChange(any(Repository.class), any(Settings.class), any(RefChange.class)))
				.thenReturn(Lists.newArrayList("error with commit"));

		boolean allowed = yaccHook.onReceive(repositoryHookContext, Lists.newArrayList(mock(RefChange.class)), hookResponse);
		assertThat(allowed).isFalse();
	}

	@Test
	public void testOnReceive_errorsArePrintedToHookStdErr()
	{
		when(yaccService.checkRefChange(any(Repository.class), any(Settings.class), any(RefChange.class)))
				.thenReturn(Lists.newArrayList("error1", "error2"));

		yaccHook.onReceive(repositoryHookContext, Lists.newArrayList(mock(RefChange.class)), hookResponse);

		//verify(hookResponse.err()).println(YaccHook.ERROR_BEARS);
		verify(hookResponse.err()).println("error1");
		verify(hookResponse.err()).println("error2");
	}


}
