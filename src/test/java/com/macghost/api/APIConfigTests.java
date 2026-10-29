package com.macghost.api;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class APIConfigTests {

    /* retorna uma fonte de dados com mocks para edição */
    @SuppressWarnings("unchecked")
    private Supplier<InputStream> fonteSimulada() {
        return mock(Supplier.class);
    }

    private InputStream propriedades(String conteudo) {
        return new ByteArrayInputStream(conteudo.getBytes(StandardCharsets.ISO_8859_1));
    }

    /* teste para verificar a leitura correta da chave da API do arquivo .properties */
    @Test
    void deveLerChaveDasPropriedadesSemArquivoReal() {
        Supplier<InputStream> fonte = fonteSimulada();
        when(fonte.get()).thenReturn(propriedades("api.key=chave-de-teste\nambiente=local\n"));

        APIConfig config = new APIConfig(fonte);

        assertEquals("chave-de-teste", config.getApiKey());
        verify(fonte, times(1)).get();
    }

    @Test
    void deveRemoverEspacosExternosDaChave() {
        Supplier<InputStream> fonte = fonteSimulada();
        when(fonte.get()).thenReturn(propriedades("api.key=  chave-teste  \n"));

        assertEquals("chave-teste", new APIConfig(fonte).getApiKey());
    }

    @Test
    void deveFecharOFluxoAposCarregar() throws Exception {
        Supplier<InputStream> fonte = fonteSimulada();
        InputStream dados = spy(propriedades("api.key=segredo-falso\n"));
        when(fonte.get()).thenReturn(dados);

        new APIConfig(fonte);

        verify(dados).close();
    }

    @Test
    void deveRejeitarArquivoNaoEncontrado() {
        Supplier<InputStream> fonte = fonteSimulada();
        when(fonte.get()).thenReturn(null);

        IllegalStateException erro = assertThrows(IllegalStateException.class,
                () -> new APIConfig(fonte));

        assertTrue(erro.getMessage().contains("não encontrado"));
    }

    @Test
    void deveRejeitarPropriedadeAusente() {
        Supplier<InputStream> fonte = fonteSimulada();
        when(fonte.get()).thenReturn(propriedades("outra.chave=valor\n"));

        IllegalStateException erro = assertThrows(IllegalStateException.class,
                () -> new APIConfig(fonte));

        assertTrue(erro.getMessage().contains("api.key"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"api.key=\n", "api.key=    \n"})
    void deveRejeitarChaveVaziaOuEmBranco(String conteudo) {
        Supplier<InputStream> fonte = fonteSimulada();
        when(fonte.get()).thenReturn(propriedades(conteudo));

        assertThrows(IllegalStateException.class, () -> new APIConfig(fonte));
    }

    @Test
    void devePreservarACausaDaFalhaDeLeitura() throws Exception {
        Supplier<InputStream> fonte = fonteSimulada();
        InputStream dados = mock(InputStream.class);
        when(fonte.get()).thenReturn(dados);
        when(dados.read(any(byte[].class)))
                .thenThrow(new IOException("falha de leitura simulada"));

        IllegalStateException erro = assertThrows(IllegalStateException.class,
                () -> new APIConfig(fonte));

        assertInstanceOf(IOException.class, erro.getCause());
        verify(dados).close();
    }

    @Test
    void deveRejeitarFonteNula() {
        assertThrows(NullPointerException.class,
                () -> new APIConfig((Supplier<InputStream>) null));
    }

    @Test
    void deveLerChaveWeatherSiteApiKey() {
        Supplier<InputStream> fonte = fonteSimulada();
        when(fonte.get()).thenReturn(propriedades("WEATHER_SITE_API_KEY=minha-chave-env\n"));

        APIConfig config = new APIConfig(fonte);

        assertEquals("minha-chave-env", config.getApiKey());
    }

    @Test
    void deveCarregarChaveDoArquivoEnvPadraoQuandoPresente() {
        if (new java.io.File(".env").exists()) {
            APIConfig config = new APIConfig();
            assertNotNull(config.getApiKey());
            assertFalse(config.getApiKey().isBlank());
        }
    }
}
