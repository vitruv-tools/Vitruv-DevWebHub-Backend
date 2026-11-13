package tools.vitruv.framework.remote.client.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.vitruv.framework.views.CommittableView;
import tools.vitruv.framework.views.ViewSelector;

/**
 * Annotations on remote committable views are stored on the underlying {@link RemoteView} instead
 * of being forwarded to a newly created trait wrapper (which recursed until a
 * {@link StackOverflowError} or failed on modified views).
 */
class RemoteViewAnnotationTest {
  private static final String VIEW_UUID = "view-1";

  private RemoteView remoteView;

  @BeforeEach
  void setUp() {
    var connection = mock(VitruvRemoteConnection.class);
    when(connection.isViewClosed(VIEW_UUID)).thenReturn(false);
    when(connection.isViewOutdated(VIEW_UUID)).thenReturn(false);

    var resourceSet = new ResourceSetImpl();
    resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
        .put("*", new XMIResourceFactoryImpl());

    remoteView = new RemoteView(VIEW_UUID, resourceSet, mock(ViewSelector.class), connection);
  }

  @Test
  void changeRecordingViewStoresAnnotations() {
    assertRoundTrip(remoteView.withChangeRecordingTrait());
  }

  @Test
  void changeDerivingViewStoresAnnotations() {
    assertRoundTrip(remoteView.withChangeDerivingTrait());
  }

  @Test
  void annotationsWorkOnModifiedView() {
    CommittableView view = remoteView.withChangeRecordingTrait();
    view.registerRoot(EcoreFactory.eINSTANCE.createEPackage(), URI.createURI("test.ecore"));
    assertThat(view.isModified()).isTrue();

    view.setAnnotation(String.class, "author");

    assertThat(view.getAnnotation(String.class)).contains("author");
  }

  @Test
  void annotationsSurviveTraitSwitch() {
    CommittableView recording = remoteView.withChangeRecordingTrait();
    recording.setAnnotation(String.class, "author");

    CommittableView deriving = recording.withChangeDerivingTrait();
    assertThat(deriving.getAnnotation(String.class)).contains("author");

    deriving.setAnnotation(Integer.class, 42);
    assertThat(deriving.withChangeRecordingTrait().getAnnotation(Integer.class)).contains(42);
  }

  @Test
  void changeRecordingTraitOfRecordingViewIsRecording() {
    CommittableView recording = remoteView.withChangeRecordingTrait();

    assertThat(recording.withChangeRecordingTrait()).isInstanceOf(ChangeRecordingRemoteView.class);
  }

  private static void assertRoundTrip(CommittableView view) {
    assertThat(view.getAnnotation(String.class)).isEmpty();

    view.setAnnotation(String.class, "author");
    view.setAnnotation(Integer.class, 42);

    assertThat(view.getAnnotation(String.class)).contains("author");
    assertThat(view.getAnnotation(Integer.class)).contains(42);
    assertThat(view.getAnnotation(Long.class)).isEmpty();
    assertThat(view.getAnnotations()).containsEntry(String.class, "author").hasSize(2);
    assertThatThrownBy(() -> view.getAnnotations().clear())
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
