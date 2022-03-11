/*
  Copyright (C) 2021 Kyligence Inc. All rights reserved.

  http://kyligence.io

  This software is the confidential and proprietary information of
  Kyligence Inc. ("Confidential Information"). You shall not disclose
  such Confidential Information and shall use it only in accordance
  with the terms of the license agreement you entered into with
  Kyligence Inc.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
import { connect } from 'react-redux';
import mapReselectToProps from './reselect';
import mapDispatchToProps from './actions';

function isUndefined(value) {
  return value === undefined;
}

function withDefaultValue(stateValue, defaultValue) {
  return isUndefined(stateValue) ? defaultValue : stateValue;
}

function getMapStoreToProps(namespace = '', mapState = {}, mapReselect = {}, defaultState = {}) {
  return function mapStoreToProps(state) {
    const stateToProps = {};
    const reselectToProps = {};
    const allReselect = mapReselectToProps(state);
    const namespaceArray = namespace.split('/').filter(item => !!item);

    for (const stateKey of Object.keys(mapState)) {
      const namespacedState = namespaceArray.reduce(
        (currentState, namespaceKey) => currentState[namespaceKey], state,
      ) || {};

      const defaultValue = defaultState[stateKey];
      stateToProps[stateKey] = withDefaultValue(
        mapState[stateKey](namespacedState), defaultValue,
      );
    }

    for (const selectKey of Object.keys(mapReselect)) {
      const defaultValue = defaultState[selectKey];
      reselectToProps[selectKey] = withDefaultValue(
        mapReselect[selectKey](allReselect), defaultValue,
      );
    }

    return { ...stateToProps, ...reselectToProps };
  };
}

export function Connect(...args) {
  return component => {
    const mappers = args.map(params => {
      const { namespace, mapState, mapReselect, defaultState } = params;
      return getMapStoreToProps(namespace, mapState, mapReselect, defaultState);
    });

    const options = args.reduce((allOptions, params) => (
      { ...allOptions, ...(params.options || {}) }
    ), {});

    const mapStateToProps = store => mappers.reduce((states, mapper) => (
      { ...states, ...mapper(store) }
    ), {});

    const reduxComponent = connect(mapStateToProps, mapDispatchToProps, null, options)(component);
    reduxComponent.jest = connect(mapStateToProps, null, null, options)(component);
    return reduxComponent;
  };
}
