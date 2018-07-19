package org.ballerinax.kubernetes.test.samples;

import io.fabric8.docker.api.model.ImageInspect;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ballerinax.kubernetes.KubernetesConstants;
import org.ballerinax.kubernetes.exceptions.KubernetesPluginException;
import org.ballerinax.kubernetes.test.utils.KubernetesTestUtils;
import org.ballerinax.kubernetes.utils.KubernetesUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

import static org.ballerinax.kubernetes.KubernetesConstants.DOCKER;
import static org.ballerinax.kubernetes.KubernetesConstants.KUBERNETES;
import static org.ballerinax.kubernetes.test.utils.KubernetesTestUtils.getDockerImage;

public class TestSample1 implements SampleTester {

    private static final Log log = LogFactory.getLog(TestSample1.class);
    private final String sourceDirPath = SAMPLE_DIR + File.separator + "sample1";
    private final String targetPath = sourceDirPath + File.separator + KUBERNETES;
    private final String dockerImage = "hello_world_k8s:latest";

    @BeforeClass
    public void compileSample() throws IOException, InterruptedException {
        Assert.assertEquals(KubernetesTestUtils.compileBallerinaFile(sourceDirPath, "hello_world_k8s.bal"), 0);
    }

    @Test
    public void validateDeployment() throws IOException {
        File deploymentYAML = new File(targetPath + File.separator + "hello_world_k8s_deployment.yaml");
        Assert.assertTrue(deploymentYAML.exists());
        Deployment deployment = KubernetesHelper.loadYaml(deploymentYAML);
        Assert.assertEquals("hello-world-k8s-deployment", deployment.getMetadata().getName());
        Assert.assertEquals(1, deployment.getSpec().getReplicas().intValue());
        Assert.assertEquals("hello_world_k8s", deployment.getMetadata().getLabels().get(KubernetesConstants
                .KUBERNETES_SELECTOR_KEY));
        Assert.assertEquals(1, deployment.getSpec().getTemplate().getSpec().getContainers().size());
        Container container = deployment.getSpec().getTemplate().getSpec().getContainers().get(0);
        Assert.assertEquals(dockerImage, container.getImage());
        Assert.assertEquals(KubernetesConstants.ImagePullPolicy.IfNotPresent.name(), container.getImagePullPolicy());
        Assert.assertEquals(1, container.getPorts().size());
        Assert.assertEquals(0, container.getEnv().size());
    }

    @Test
    public void validateK8SService() throws IOException {
        File serviceYAML = new File(targetPath + File.separator + "hello_world_k8s_svc.yaml");
        Assert.assertTrue(serviceYAML.exists());
        Service service = KubernetesHelper.loadYaml(serviceYAML);
        Assert.assertEquals("helloworld-svc", service.getMetadata().getName());
        Assert.assertEquals("hello_world_k8s", service.getMetadata().getLabels().get(KubernetesConstants
                .KUBERNETES_SELECTOR_KEY));
        Assert.assertEquals(KubernetesConstants.ServiceType.NodePort.name(), service.getSpec().getType());
        Assert.assertEquals(1, service.getSpec().getPorts().size());
        Assert.assertEquals(9090, service.getSpec().getPorts().get(0).getPort().intValue());
    }

    @Test
    public void validateDockerfile() {
        File dockerFile = new File(targetPath + File.separator + DOCKER + File.separator + "Dockerfile");
        Assert.assertTrue(dockerFile.exists());
    }

    @Test
    public void validateDockerImage() {
        ImageInspect imageInspect = getDockerImage(dockerImage);
        Assert.assertEquals(1, imageInspect.getContainerConfig().getExposedPorts().size());
        Assert.assertEquals("9090/tcp", imageInspect.getContainerConfig().getExposedPorts().keySet().toArray()[0]);
    }

    @AfterClass
    public void cleanUp() throws KubernetesPluginException {
        KubernetesUtils.deleteDirectory(targetPath);
        KubernetesTestUtils.deleteDockerImage(dockerImage);
    }
}
