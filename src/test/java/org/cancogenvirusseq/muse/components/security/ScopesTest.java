/*
 * Copyright (c) 2021 The Ontario Institute for Cancer Research. All rights reserved
 *
 * This program and the accompanying materials are made available under the terms of the GNU Affero General Public License v3.0.
 * You should have received a copy of the GNU Affero General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.cancogenvirusseq.muse.components.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.stream.Collectors;
import lombok.val;
import org.cancogenvirusseq.muse.config.websecurity.AuthProperties;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class ScopesTest {
  private final Scopes scopes;

  public ScopesTest() {

    val authProperties = new AuthProperties();

    authProperties.getScopes().setSystem("test.WRITE");
    authProperties.getScopes().getStudy().setPrefix("muse.");
    authProperties.getScopes().getStudy().setSuffix(".WRITE");

    this.scopes = new Scopes(authProperties);
  }

  @Test
  public void systemScopeAllowed() {
    assertThat(scopes.isValidScope.test("test.WRITE")).isTrue();
  }

  @Test
  public void systemScopeNotAllowed() {
    assertThat(scopes.isValidScope.test("test.READ")).isFalse();
    assertThat(scopes.isValidScope.test("other.WRITE")).isFalse();
  }

  @Test
  public void studyScopeAllowed() {
    assertThat(scopes.isValidScope.test("muse.TEST-STUDY.WRITE")).isTrue();
    assertThat(scopes.isValidScope.test("muse.other-test-study.WRITE")).isTrue();
  }

  @Test
  public void studyScopeNotAllowed() {
    assertThat(scopes.isValidScope.test("someApp.TEST-STUDY.WRITE")).isFalse();
    assertThat(scopes.isValidScope.test("muse.other-test-study.READ")).isFalse();
  }

  @Test
  public void scopesCaseSensitive() {
    assertThat(scopes.isValidScope.test("TEST.WRITE")).isFalse();
    assertThat(scopes.isValidScope.test("test.write")).isFalse();
    assertThat(scopes.isValidScope.test("MUSE.TEST-STUDY.WRITE")).isFalse();
    assertThat(scopes.isValidScope.test("muse.other-test-study.write")).isFalse();
  }

  @Test
  public void nonMuseScopesFilteredFromWrapWithUserScopes() {
    val auth = mock(AbstractAuthenticationToken.class, RETURNS_DEEP_STUBS);
    when(auth.getAuthorities())
        .thenReturn(
            List.of(
                new SimpleGrantedAuthority("test.WRITE"),
                new SimpleGrantedAuthority("muse.TEST-STUDY.WRITE")));

    val listFunc =
        scopes.wrapWithUserScopes(
            (List<String> scopes, List<String> userScopes) ->
                scopes.stream()
                    .filter(
                        scope -> userScopes.stream().anyMatch(authority -> authority.equals(scope)))
                    .collect(Collectors.toList()),
            auth);

    // assert system and study scopes correctly passed through
    assertThat(listFunc.apply(List.of("test.WRITE")))
        .containsExactlyElementsOf(List.of("test.WRITE"));
    assertThat(listFunc.apply(List.of("muse.TEST-STUDY.WRITE")))
        .containsExactlyElementsOf(List.of("muse.TEST-STUDY.WRITE"));
    assertThat(listFunc.apply(List.of("test.WRITE", "muse.TEST-STUDY.WRITE")))
        .containsExactlyElementsOf(List.of("test.WRITE", "muse.TEST-STUDY.WRITE"));

    // assert other scopes removed
    assertThat(
            listFunc.apply(List.of("test.WRITE", "muse.TEST-STUDY.WRITE", "song.TEST-STUDY.WRITE")))
        .containsExactlyElementsOf(List.of("test.WRITE", "muse.TEST-STUDY.WRITE"));
    assertThat(listFunc.apply(List.of("test.WRITE", "muse.TEST-STUDY.WRITE", "otherApp.WRITE")))
        .containsExactlyElementsOf(List.of("test.WRITE", "muse.TEST-STUDY.WRITE"));
  }
}
