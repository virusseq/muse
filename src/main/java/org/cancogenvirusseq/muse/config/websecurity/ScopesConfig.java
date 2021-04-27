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

package org.cancogenvirusseq.muse.config.websecurity;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "auth.scopes")
public class ScopesConfig {

  private final String system;
  private final StudyScopeConfig study = new StudyScopeConfig();
  private Predicate<String> isValidScope;

  private void init() {
    final Predicate<String> startsWithStudyPrefix =
        (String scope) -> scope.startsWith(study.getPrefix());

    final Predicate<String> endsWithStudySuffix =
        (String scope) -> scope.endsWith(study.getSuffix());

    final Predicate<String> isStudyScope = startsWithStudyPrefix.and(endsWithStudySuffix);

    final Predicate<String> isSystemScope = (String scope) -> scope.equals(system);

    this.isValidScope = isSystemScope.or(isStudyScope);
  }

  @Getter
  @Setter
  public static class StudyScopeConfig {

    @NotNull
    @Pattern(regexp = "^\\w+\\W$")
    private String prefix;

    @NotNull
    @Pattern(regexp = "^\\W\\w+$")
    private String suffix;
  }

  @Bean
  public Function<Authentication, Boolean> readWriteScopeChecker() {
    return authentication ->
        authentication.getAuthorities().stream().map(Objects::toString).anyMatch(isValidScope);
  }
}
