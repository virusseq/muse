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

package org.cancogenvirusseq.muse.utils;

import java.util.function.BiFunction;
import java.util.function.Function;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class SecurityContextWrapper {
  public static <T, R> Function<T, Mono<R>> forMono(
      BiFunction<T, SecurityContext, Mono<R>> biFunctionToWrap) {
    return (T t) ->
        ReactiveSecurityContextHolder.getContext()
            .flatMap(securityContext -> biFunctionToWrap.apply(t, securityContext));
  }

  public static <T, R> Function<T, Flux<R>> forFlux(
      BiFunction<T, SecurityContext, Flux<R>> biFunctionToWrap) {
    return (T t) ->
        ReactiveSecurityContextHolder.getContext()
            .flatMapMany(securityContext -> biFunctionToWrap.apply(t, securityContext));
  }

  public static <T, U, R> BiFunction<T, U, Flux<R>> forFlux(
      TriFunction<T, U, SecurityContext, Flux<R>> triFunctionToWrap) {
    return (T t, U u) ->
        ReactiveSecurityContextHolder.getContext()
            .flatMapMany(securityContext -> triFunctionToWrap.apply(t, u, securityContext));
  }
}
