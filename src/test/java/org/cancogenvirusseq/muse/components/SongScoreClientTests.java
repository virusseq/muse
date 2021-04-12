///*
// * Copyright (c) 2021 The Ontario Institute for Cancer Research. All rights reserved
// *
// * This program and the accompanying materials are made available under the terms of the GNU Affero General Public License v3.0.
// * You should have received a copy of the GNU Affero General Public License along with
// * this program. If not, see <http://www.gnu.org/licenses/>.
// *
// * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
// * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
// * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
// * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
// * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
// * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
// * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
// */
//
//package org.cancogenvirusseq.muse.components;
//
//import static org.mockito.Mockito.when;
//
//import org.cancogenvirusseq.muse.model.song_score.SubmitResponse;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.web.reactive.function.client.WebClient;
//import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;
//import org.springframework.web.reactive.function.client.WebClient.RequestHeadersUriSpec;
//import reactor.core.publisher.Mono;
//
//@ExtendWith(MockitoExtension.class)
//public class SongScoreClientTests {
//
//  static final String STUDY_ID = "TEST-CASE";
//
//  SongScoreClient songScoreClient;
//
//  @Mock private WebClient songClient;
//  @Mock private WebClient scoreClient;
//  @Mock private RequestHeadersSpec requestHeadersMock;
//  @Mock private RequestHeadersUriSpec requestHeadersUriMock;
//  @Mock private WebClient.RequestBodySpec requestBodyMock;
//  @Mock private WebClient.RequestBodyUriSpec requestBodyUriMock;
//  @Mock private WebClient.ResponseSpec responseMock;
//
//  @BeforeEach
//  void setUp() {
//    songScoreClient = new SongScoreClient(songClient, scoreClient);
//  }
//
//  @Test
//  void submitExceptionMapping() {
//    SubmitResponse badRequest = new SubmitResponse("test-id", "400");
//    when(songClient.post()).thenReturn(requestBodyUriMock);
//    when(requestBodyUriMock.uri("/submit/TEST-CASE")).thenReturn(requestBodyMock);
//    when(requestBodyMock.bodyValue(badRequest)).thenReturn(requestHeadersMock);
//    when(requestHeadersMock.retrieve()).thenReturn(responseMock);
//    when(responseMock.bodyToMono(SubmitResponse.class)).thenReturn(Mono.just(badRequest));
//
//    //    Mono<SubmitResponse> submitBadRequest = songScoreClient.submitPayload("TEST-CASE")
//  }
//}
